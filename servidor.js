const WebSocket = require('ws');
const { Pool } = require('pg');
const crypto = require('crypto');
const http = require('http');
const fs = require('fs');
const path = require('path');

// Configuración hardcodeada (sin variables de entorno)
const DATABASE_URL = "postgresql://seend_z27i_user:aLDJZC5185nTaGXiw2rZuCT2qcH6HQgB@dpg-dbh266bl550s738fk6lg-a/seend_z27i";
const S3_PUBLIC_URL = "https://s3.todus.cu/stream/seend";
const PORT = 8000;

const pool = new Pool({ connectionString: DATABASE_URL });
const clientes = new Map();
const noLeidos = new Map();

// --- Rate limiting en login ---
const intentosLogin = new Map();
const MAX_INTENTOS = 5;
const VENTANA_MS = 15 * 60 * 1000;

function rateLimitLogin(ip) {
    const ahora = Date.now();
    const intentos = (intentosLogin.get(ip) || []).filter(t => ahora - t < VENTANA_MS);
    if (intentos.length >= MAX_INTENTOS) {
        intentosLogin.set(ip, intentos);
        return false;
    }
    intentos.push(ahora);
    intentosLogin.set(ip, intentos);
    return true;
}

// --- Inicialización de la Base de Datos (limpia y recrea) ---
async function initDB() {
    try {
        console.log("🧹 Limpiando base de datos...");
        await pool.query(`
            DROP TABLE IF EXISTS mensajes CASCADE;
            DROP TABLE IF EXISTS sesiones CASCADE;
            DROP TABLE IF EXISTS usuarios CASCADE;
        `);
        console.log("✔ Tablas eliminadas.");

        await pool.query(`
        CREATE TABLE usuarios (
            nombre TEXT PRIMARY KEY, salt TEXT NOT NULL, hash TEXT NOT NULL,
            bio TEXT DEFAULT '', avatar_url TEXT DEFAULT '',
            ultima_conexion DOUBLE PRECISION DEFAULT 0
        );
        CREATE TABLE mensajes (
            id TEXT PRIMARY KEY, de TEXT NOT NULL, para TEXT NOT NULL,
            texto TEXT DEFAULT '', imagen_url TEXT DEFAULT '', imagen_tamano TEXT DEFAULT '',
            imagen_dim TEXT DEFAULT '', blurhash TEXT DEFAULT '',
            estado TEXT DEFAULT 'pendiente', hora TEXT, ts DOUBLE PRECISION,
            cita_autor TEXT, cita_texto TEXT, oculto_para TEXT DEFAULT ''
        );
        CREATE TABLE sesiones (
            token TEXT PRIMARY KEY, nombre TEXT NOT NULL
        );
        CREATE INDEX idx_msg_conv ON mensajes (de, para, ts);
        CREATE INDEX idx_msg_para ON mensajes (para, estado);
        `);
        console.log("✔ Tablas recreadas correctamente.");
    } catch (e) {
        console.error("❌ Error inicializando la BD:", e);
    }
}

async function dbUsuario(nombre) {
    const res = await pool.query("SELECT nombre,salt,hash,bio,avatar_url,ultima_conexion FROM usuarios WHERE nombre=$1", [nombre]);
    return res.rows[0];
}
async function dbCrearUsuario(nombre, salt, hash) {
    const res = await pool.query(
        `INSERT INTO usuarios (nombre, salt, hash, bio, ultima_conexion)
         VALUES ($1, $2, $3, $4, $5)
         ON CONFLICT (nombre) DO NOTHING`,
        [nombre, salt, hash, "¡Hola! Estoy usando Seend", Date.now() / 1000]
    );
    return res.rowCount > 0;
}
async function dbActualizarPerfil(nombre, bio, avatarUrl) {
    await pool.query("UPDATE usuarios SET bio=$1,avatar_url=$2 WHERE nombre=$3", [bio, avatarUrl, nombre]);
}
async function dbRenombrar(viejo, nuevo) {
    const client = await pool.connect();
    try {
        await client.query("BEGIN");
        await client.query("UPDATE usuarios SET nombre=$1 WHERE nombre=$2", [nuevo, viejo]);
        await client.query("UPDATE mensajes SET de=$1 WHERE de=$2", [nuevo, viejo]);
        await client.query("UPDATE mensajes SET para=$1 WHERE para=$2", [nuevo, viejo]);
        await client.query("UPDATE sesiones SET nombre=$1 WHERE nombre=$2", [nuevo, viejo]);
        await client.query("COMMIT");
    } catch (e) {
        await client.query("ROLLBACK");
        throw e;
    } finally {
        client.release();
    }
}
async function dbSesionCrear(token, nombre) {
    await pool.query("INSERT INTO sesiones (token,nombre) VALUES ($1,$2)", [token, nombre]);
}
async function dbSesion(token) {
    const res = await pool.query("SELECT nombre FROM sesiones WHERE token=$1", [token]);
    return res.rows[0] ? res.rows[0].nombre : null;
}
async function dbInsertarMsg(msg) {
    await pool.query(`INSERT INTO mensajes (id,de,para,texto,imagen_url,imagen_tamano,imagen_dim,blurhash,estado,hora,ts,cita_autor,cita_texto,oculto_para)
        VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14)`,
        [msg.id, msg.de, msg.para, msg.texto, msg.imagen_url, msg.imagen_tamano, msg.imagen_dim, msg.blurhash, msg.estado, msg.hora, msg.ts, msg.cita_autor, msg.cita_texto, msg.oculto_para]);
}
async function dbBorrarMsg(id) {
    await pool.query("DELETE FROM mensajes WHERE id=$1", [id]);
}
async function dbMensajesPendientes(para) {
    const res = await pool.query("SELECT * FROM mensajes WHERE para=$1 AND estado='pendiente' ORDER BY ts ASC", [para]);
    return res.rows;
}
async function dbListaUsuarios() {
    const res = await pool.query("SELECT nombre,avatar_url,ultima_conexion FROM usuarios");
    return res.rows;
}

function hashPassword(password, salt = crypto.randomBytes(16).toString('hex')) {
    const hash = crypto.pbkdf2Sync(password, Buffer.from(salt, 'hex'), 120000, 64, 'sha256').toString('hex');
    return { salt, hash };
}
function verificarPassword(password, salt, hash) {
    try {
        const { hash: hashCalculado } = hashPassword(password, salt);
        return crypto.timingSafeEqual(Buffer.from(hash, 'hex'), Buffer.from(hashCalculado, 'hex'));
    } catch (_) {
        return false;
    }
}
function online(nombre) {
    for (let [ws, n] of clientes) { if (n === nombre) return true; }
    return false;
}
async function enviarA(nombre, payload) {
    const data = JSON.stringify(payload);
    for (let [ws, n] of clientes) {
        if (n === nombre && ws.readyState === WebSocket.OPEN) {
            try { ws.send(data); } catch (_) { clientes.delete(ws); }
        }
    }
}
async function broadcast(payload) {
    const data = JSON.stringify(payload);
    for (let [ws] of clientes) {
        if (ws.readyState === WebSocket.OPEN) {
            try { ws.send(data); } catch (_) { clientes.delete(ws); }
        }
    }
}
async function usuariosPayload() {
    const filas = await dbListaUsuarios();
    return filas.map(f => ({ nombre: f.nombre, avatar: f.avatar_url || "", bio: "", ultima: f.ultima_conexion || 0, en_linea: online(f.nombre) }));
}
async function enviarUsuarios() {
    await broadcast({ tipo: "usuarios", usuarios: await usuariosPayload(), en_linea: clientes.size });
}

// NUEVO: notificar presencia cuando alguien se conecta/desconecta
async function notificarPresencia(nombre, enLinea) {
    await broadcast({
        tipo: "presencia",
        usuario: nombre,
        en_linea: enLinea,
        ultima: Date.now() / 1000
    });
}

function esUrlS3Valida(url) {
    if (!url || typeof url !== "string") return false;
    return url.startsWith(S3_PUBLIC_URL + "/");
}

// --- Servidor HTTP ---
const server = http.createServer((req, res) => {
    if (req.url === "/healthz") {
        res.writeHead(200, { "Content-Type": "text/plain" });
        return res.end("ok");
    }

    if (req.url === "/download") {
        const apkPath = path.join(__dirname, "seend.apk");
        if (fs.existsSync(apkPath)) {
            const stat = fs.statSync(apkPath);
            res.writeHead(200, {
                "Content-Type": "application/vnd.android.package-archive",
                "Content-Length": stat.size,
                "Content-Disposition": 'attachment; filename="seend.apk"'
            });
            return fs.createReadStream(apkPath).pipe(res);
        }
        res.writeHead(302, { Location: S3_PUBLIC_URL + "/seend.apk" });
        return res.end();
    }

    if (req.url === "/" || req.url === "/index.html") {
        const indexPath = path.join(__dirname, "index.html");
        if (fs.existsSync(indexPath)) {
            res.writeHead(200, { "Content-Type": "text/html; charset=utf-8" });
            return res.end(fs.readFileSync(indexPath));
        }
        res.writeHead(404);
        return res.end("index.html no encontrado");
    }

    res.writeHead(404, { "Content-Type": "text/plain" });
    res.end("Not found");
});

// --- WebSocket integrado con path '/ws' ---
initDB().then(() => {
    const wss = new WebSocket.Server({ server, path: '/ws' });

    wss.on('connection', (ws) => {
        let nombre = null;
        let autenticado = false;
        let heartbeatInterval = null;

        // Heartbeat: detectar conexiones muertas
        heartbeatInterval = setInterval(() => {
            if (ws.readyState === WebSocket.OPEN) {
                ws.ping();
            }
        }, 30000);

        ws.on('message', async (message) => {
            try {
                const m = JSON.parse(message);
                const tipo = m.tipo;

                if (tipo === "registro") {
                    const n = (m.nombre || "").trim().slice(0, 20);
                    const p = m.password || "";
                    if (!n || p.length < 4) return ws.send(JSON.stringify({ tipo: "error", texto: "Nombre y contraseña (mín. 4)" }));
                    const { salt, hash } = hashPassword(p);
                    const insertado = await dbCrearUsuario(n, salt, hash);
                    if (!insertado) return ws.send(JSON.stringify({ tipo: "error", texto: "Nombre ya registrado" }));
                    const token = crypto.randomBytes(24).toString('base64url');
                    await dbSesionCrear(token, n);
                    return ws.send(JSON.stringify({ tipo: "auth_ok", token, nombre: n, bio: "¡Hola! Estoy usando Seend", avatar: "" }));
                }

                if (tipo === "login") {
                    const ip = ws._socket?.remoteAddress || "unknown";
                    if (!rateLimitLogin(ip)) {
                        return ws.send(JSON.stringify({ tipo: "error", texto: "Demasiados intentos. Espera 15 minutos." }));
                    }
                    const n = (m.nombre || "").trim().slice(0, 20);
                    const p = m.password || "";
                    const u = await dbUsuario(n);
                    if (!u || !verificarPassword(p, u.salt, u.hash)) return ws.send(JSON.stringify({ tipo: "error", texto: "Credenciales incorrectas" }));
                    const token = crypto.randomBytes(24).toString('base64url');
                    await dbSesionCrear(token, n);
                    return ws.send(JSON.stringify({ tipo: "auth_ok", token, nombre: n }));
                }

                if (tipo === "validar_sesion") {
                    const n = await dbSesion(m.token || "");
                    if (!n) return ws.send(JSON.stringify({ tipo: "sesion_invalida" }));
                    return ws.send(JSON.stringify({ tipo: "sesion_valida", nombre: n }));
                }

                if (tipo === "entrar") {
                    const n = await dbSesion(m.token || "");
                    if (!n) return ws.send(JSON.stringify({ tipo: "sesion_invalida" }));
                    nombre = n; autenticado = true;
                    for (let [w, quien] of clientes) {
                        if (quien === nombre) { try { w.close(); } catch(e) {} clientes.delete(w); }
                    }
                    clientes.set(ws, nombre);

                    // NUEVO: notificar presencia a todos
                    await notificarPresencia(nombre, true);

                    const pendientes = await dbMensajesPendientes(nombre);
                    for (let p of pendientes) {
                        const msg = { id: p.id, de: p.de, para: p.para, texto: p.texto || "", imagen_url: p.imagen_url || "", imagen_tamano: p.imagen_tamano || "", imagen_dim: p.imagen_dim || "", blurhash: p.blurhash || "", estado: "entregado", hora: p.hora || "", ts: p.ts || 0, cita: p.cita_autor ? { autor: p.cita_autor, texto: p.cita_texto } : null };
                        ws.send(JSON.stringify({ tipo: "mensaje", mensaje: msg }));
                        await enviarA(p.de, { tipo: "estados", ids: [p.id], estado: "entregado" });
                        await dbBorrarMsg(p.id);
                    }
                    await enviarUsuarios();
                    return;
                }

                if (!autenticado) return ws.send(JSON.stringify({ tipo: "error", texto: "No autenticado" }));

                if (tipo === "pedir_usuarios") {
                    return ws.send(JSON.stringify({ tipo: "usuarios", usuarios: await usuariosPayload(), en_linea: clientes.size }));
                }

                if (tipo === "abrir_chat") {
                    const otro = m.otro;
                    if (!otro) return;
                    const pendientes = await dbMensajesPendientes(nombre);
                    for (let p of pendientes) {
                        const msg = { id: p.id, de: p.de, para: p.para, texto: p.texto || "", imagen_url: p.imagen_url || "", imagen_tamano: p.imagen_tamano || "", imagen_dim: p.imagen_dim || "", blurhash: p.blurhash || "", estado: "entregado", hora: p.hora || "", ts: p.ts || 0, cita: p.cita_autor ? { autor: p.cita_autor, texto: p.cita_texto } : null };
                        ws.send(JSON.stringify({ tipo: "mensaje", mensaje: msg }));
                        await enviarA(p.de, { tipo: "estados", ids: [p.id], estado: "entregado" });
                        await dbBorrarMsg(p.id);
                    }
                    await enviarA(otro, { tipo: "estados", ids: [], estado: "visto" });
                    const u = await dbUsuario(otro);
                    if (u) ws.send(JSON.stringify({ tipo: "perfil", nombre: u.nombre, bio: u.bio || "", avatar: u.avatar_url || "", en_linea: online(otro), ultima: u.ultima_conexion || 0 }));
                    return;
                }

                if (tipo === "mensaje") {
                    const para = m.para;
                    const texto = (m.texto || "").trim();
                    const imagen_url = m.imagen_url;
                    const imagen_tamano = m.imagen_tamano || "";
                    const imagen_dim = m.imagen_dim || "";
                    const blurhash = m.blurhash || "";
                    const resp = m.responder_a;
                    if (!para || (!texto && !imagen_url)) return;

                    if (!(await dbUsuario(para))) {
                        return ws.send(JSON.stringify({ tipo: "error", texto: "El usuario destinatario no existe" }));
                    }
                    if (imagen_url && !esUrlS3Valida(imagen_url)) {
                        return ws.send(JSON.stringify({ tipo: "error", texto: "URL de imagen no autorizada" }));
                    }

                    let cita_autor = null, cita_texto = null;
                    if (resp) {
                        const r = await pool.query("SELECT de, texto FROM mensajes WHERE id=$1", [resp]);
                        if (r.rows[0]) { cita_autor = r.rows[0].de; cita_texto = (r.rows[0].texto || "[imagen]").slice(0, 120); }
                    }
                    const estado_inicial = online(para) ? "entregado" : "pendiente";
                    const out = { id: crypto.randomUUID(), de: nombre, para: para, texto: texto.slice(0, 2000), imagen_url: imagen_url || "", imagen_tamano, imagen_dim, blurhash, hora: new Date().toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' }), ts: Date.now() / 1000, estado: estado_inicial, cita_autor, cita_texto };
                    await dbInsertarMsg(out);
                    const salida = { ...out };
                    if (cita_autor) salida.cita = { autor: cita_autor, texto: cita_texto };
                    await enviarA(nombre, { tipo: "mensaje", mensaje: salida, eco: true });
                    if (online(para)) {
                        const salidaReceptor = { ...salida, estado: "entregado" };
                        await enviarA(para, { tipo: "mensaje", mensaje: salidaReceptor });
                        await dbBorrarMsg(out.id);
                    } else {
                        if (!noLeidos.has(para)) noLeidos.set(para, {});
                        noLeidos.get(para)[nombre] = (noLeidos.get(para)[nombre] || 0) + 1;
                    }
                    return;
                }

                if (tipo === "eliminar_chat") {
                    const otro = m.otro;
                    if (otro) await pool.query("UPDATE mensajes SET oculto_para=$1 WHERE (de=$2 AND para=$3) OR (de=$4 AND para=$5)", [nombre, nombre, otro, otro, nombre]);
                    return;
                }

                if (tipo === "escribiendo") {
                    const para = m.para;
                    if (para) await enviarA(para, { tipo: "escribiendo", de: nombre, activo: m.activo || false });
                    return;
                }

                if (tipo === "actualizar_perfil") {
                    const nuevo = (m.nombre || "").trim().slice(0, 20);
                    const bio = (m.bio || "").trim().slice(0, 120);
                    const avatar_url = m.avatar_url;
                    if (!nuevo) return ws.send(JSON.stringify({ tipo: "error", texto: "Nombre requerido" }));
                    if (avatar_url !== undefined && avatar_url !== "" && !esUrlS3Valida(avatar_url)) {
                        return ws.send(JSON.stringify({ tipo: "error", texto: "URL de avatar no autorizada" }));
                    }
                    const u = await dbUsuario(nombre);
                    if (nuevo !== nombre) {
                        if (await dbUsuario(nuevo)) return ws.send(JSON.stringify({ tipo: "error", texto: "Ese nombre ya existe" }));
                        await dbRenombrar(nombre, nuevo);
                        nombre = nuevo;
                        clientes.set(ws, nuevo);
                    }
                    const avatar_final = avatar_url !== undefined ? avatar_url : (u.avatar_url || "");
                    await dbActualizarPerfil(nombre, bio, avatar_final);
                    ws.send(JSON.stringify({ tipo: "perfil_actualizado", nombre: nombre, bio: bio, avatar: avatar_final }));
                    await broadcast({ tipo: "perfil_actualizado_otro", nombre: nombre, bio: bio, avatar: avatar_final });
                    await enviarUsuarios();
                    return;
                }

                if (tipo === "pedir_perfil") {
                    const quien = m.quien || nombre;
                    const u = await dbUsuario(quien);
                    if (!u) return ws.send(JSON.stringify({ tipo: "error", texto: "Usuario no encontrado" }));
                    ws.send(JSON.stringify({ tipo: "perfil", nombre: u.nombre, bio: u.bio || "", avatar: u.avatar_url || "", en_linea: online(quien), ultima: u.ultima_conexion || 0 }));
                    return;
                }

            } catch (e) {
                console.error("Error en mensaje WS:", e);
                try {
                    ws.send(JSON.stringify({ tipo: "error", texto: "Error interno del servidor" }));
                } catch (_) {}
            }
        });

        ws.on('close', async () => {
            if (heartbeatInterval) clearInterval(heartbeatInterval);
            if (clientes.has(ws)) {
                clientes.delete(ws);
                if (nombre) {
                    try {
                        noLeidos.delete(nombre);
                        // NUEVO: notificar que se desconectó
                        await notificarPresencia(nombre, false);
                        await pool.query("UPDATE usuarios SET ultima_conexion=$1 WHERE nombre=$2", [Date.now() / 1000, nombre]);
                        await enviarUsuarios();
                    } catch (e) {
                        console.error("Error en close:", e);
                    }
                }
            }
        });

        ws.on('error', (err) => {
            console.error("WS error:", err.message);
        });
    });

    server.listen(PORT, () => {
        console.log(`✅ Servidor HTTP+WS en puerto ${PORT}`);
        console.log(`   Health: http://localhost:${PORT}/healthz`);
        console.log(`   WebSocket: ws://localhost:${PORT}/ws`);
    });
}).catch(console.error);

// Graceful shutdown
process.on('SIGTERM', async () => {
    console.log('SIGTERM recibido, cerrando...');
    try { await pool.end(); } catch (_) {}
    process.exit(0);
});
process.on('SIGINT', async () => {
    console.log('SIGINT recibido, cerrando...');
    try { await pool.end(); } catch (_) {}
    process.exit(0);
});
