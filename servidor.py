import os, json, time, hashlib, secrets, uuid, asyncio
import aiohttp
from aiohttp import web, WSMsgType
from psycopg_pool import AsyncConnectionPool

DATABASE_URL = os.environ.get("DATABASE_URL", "postgresql://seend_ctgt_user:dSJjeFuoo5pB3T32sQr2jcrFchdBMu8m@dpg-dbev09psrm7s739s9fpg-a/seend_ctgt")
S3_PUBLIC_URL = os.environ.get("S3_PUBLIC_URL", "https://s3.todus.cu/stream/seend").rstrip("/")
PORT = int(os.environ.get("PORT", 8000))

pool = AsyncConnectionPool(DATABASE_URL, open=False)
clientes = {}
no_leidos = {}
urls_preparadas = {}

async def init_db():
    await pool.open()
    async with pool.connection() as conn:
        async with conn.cursor() as cur:
            await cur.execute("""
            CREATE TABLE IF NOT EXISTS usuarios (
                nombre TEXT PRIMARY KEY, salt TEXT NOT NULL, hash TEXT NOT NULL,
                bio TEXT DEFAULT '', avatar_url TEXT DEFAULT '',
                ultima_conexion DOUBLE PRECISION DEFAULT 0);""")
            await cur.execute("""
            CREATE TABLE IF NOT EXISTS mensajes (
                id TEXT PRIMARY KEY, de TEXT NOT NULL, para TEXT NOT NULL,
                texto TEXT DEFAULT '', imagen_url TEXT DEFAULT '', imagen_tamano TEXT DEFAULT '',
                imagen_dim TEXT DEFAULT '',
                estado TEXT DEFAULT 'enviado', hora TEXT, ts DOUBLE PRECISION,
                cita_autor TEXT, cita_texto TEXT,
                oculto_para TEXT DEFAULT '');""")
            await cur.execute("""
            CREATE TABLE IF NOT EXISTS sesiones (
                token TEXT PRIMARY KEY, nombre TEXT NOT NULL);""")
            await cur.execute("CREATE INDEX IF NOT EXISTS idx_msg_conv ON mensajes (de, para, ts);")
            await cur.execute("CREATE INDEX IF NOT EXISTS idx_msg_para ON mensajes (para, estado);")
            try:
                await cur.execute("ALTER TABLE mensajes ADD COLUMN IF NOT EXISTS imagen_dim TEXT DEFAULT '';")
                await cur.execute("ALTER TABLE mensajes ADD COLUMN IF NOT EXISTS oculto_para TEXT DEFAULT '';")
            except Exception: pass

async def db_usuario(n):
    async with pool.connection() as conn:
        async with conn.cursor() as cur:
            await cur.execute("SELECT nombre,salt,hash,bio,avatar_url,ultima_conexion FROM usuarios WHERE nombre=%s", (n,))
            return await cur.fetchone()

async def db_crear_usuario(n, salt, h):
    async with pool.connection() as conn:
        async with conn.cursor() as cur:
            await cur.execute("INSERT INTO usuarios (nombre,salt,hash,ultima_conexion) VALUES (%s,%s,%s,%s)", (n, salt, h, time.time()))

async def db_actualizar_perfil(n, bio, avatar_url):
    async with pool.connection() as conn:
        async with conn.cursor() as cur:
            await cur.execute("UPDATE usuarios SET bio=%s,avatar_url=%s WHERE nombre=%s", (bio, avatar_url, n))

async def db_renombrar(v, n):
    async with pool.connection() as conn:
        async with conn.cursor() as cur:
            await cur.execute("UPDATE usuarios SET nombre=%s WHERE nombre=%s", (n, v))
            await cur.execute("UPDATE mensajes SET de=%s WHERE de=%s", (n, v))
            await cur.execute("UPDATE mensajes SET para=%s WHERE para=%s", (n, v))
            await cur.execute("UPDATE sesiones SET nombre=%s WHERE nombre=%s", (n, v))

async def db_ultima(n, ts):
    async with pool.connection() as conn:
        async with conn.cursor() as cur:
            await cur.execute("UPDATE usuarios SET ultima_conexion=%s WHERE nombre=%s", (ts, n))

async def db_lista():
    async with pool.connection() as conn:
        async with conn.cursor() as cur:
            await cur.execute("SELECT nombre,avatar_url,ultima_conexion FROM usuarios")
            return await cur.fetchall()

async def db_sesion_crear(token, n):
    async with pool.connection() as conn:
        async with conn.cursor() as cur:
            await cur.execute("INSERT INTO sesiones (token,nombre) VALUES (%s,%s)", (token, n))

async def db_sesion(token):
    async with pool.connection() as conn:
        async with conn.cursor() as cur:
            await cur.execute("SELECT nombre FROM sesiones WHERE token=%s", (token,))
            r = await cur.fetchone()
            return r[0] if r else None

async def db_insertar_msg(m):
    async with pool.connection() as conn:
        async with conn.cursor() as cur:
            await cur.execute("""INSERT INTO mensajes (id,de,para,texto,imagen_url,imagen_tamano,imagen_dim,estado,hora,ts,cita_autor,cita_texto,oculto_para)
            VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)""",
            (m["id"],m["de"],m["para"],m.get("texto",""),m.get("imagen_url",""),m.get("imagen_tamano",""),
             m.get("imagen_dim",""),m.get("estado","enviado"),m.get("hora",""),m["ts"],
             m.get("cita_autor"),m.get("cita_texto"),m.get("oculto_para","")))

async def db_estado(ids, estado):
    if not ids: return
    async with pool.connection() as conn:
        async with conn.cursor() as cur:
            await cur.execute("UPDATE mensajes SET estado=%s WHERE id = ANY(%s)", (estado, list(ids)))

async def db_conv(a, b):
    async with pool.connection() as conn:
        async with conn.cursor() as cur:
            await cur.execute("""SELECT id,de,para,texto,imagen_url,imagen_tamano,imagen_dim,estado,hora,ts,cita_autor,cita_texto
            FROM mensajes WHERE ((de=%s AND para=%s) OR (de=%s AND para=%s)) AND oculto_para != %s ORDER BY ts ASC""", (a,b,b,a,a))
            return await cur.fetchall()

async def db_chats(n):
    async with pool.connection() as conn:
        async with conn.cursor() as cur:
            await cur.execute("""
            SELECT DISTINCT ON (otro) otro,de,texto,imagen_url,hora,ts,estado FROM (
              SELECT CASE WHEN de=%s THEN para ELSE de END AS otro,de,texto,imagen_url,hora,ts,estado,ts AS orden
              FROM mensajes WHERE (de=%s OR para=%s) AND oculto_para != %s
            ) s ORDER BY otro, orden DESC""", (n,n,n,n))
            return await cur.fetchall()

async def db_ocultar_chat(a, b):
    async with pool.connection() as conn:
        async with conn.cursor() as cur:
            await cur.execute("UPDATE mensajes SET oculto_para=%s WHERE (de=%s AND para=%s) OR (de=%s AND para=%s)", (a, a, b, b, a))

async def db_no_vistos(otro, n):
    async with pool.connection() as conn:
        async with conn.cursor() as cur:
            await cur.execute("SELECT id FROM mensajes WHERE de=%s AND para=%s AND estado!='visto' AND oculto_para != %s", (otro, n, n))
            return [r[0] for r in await cur.fetchall()]

def hash_pw(p, salt=None):
    if salt is None: salt = secrets.token_hex(16)
    h = hashlib.pbkdf2_hmac('sha256', p.encode(), bytes.fromhex(salt), 120_000)
    return salt, h.hex()

def verificar_pw(p, salt, h):
    _, hh = hash_pw(p, salt)
    return secrets.compare_digest(hh, h)

def online(n): return n in clientes.values()

async def enviar_a(n, payload):
    data = json.dumps(payload)
    for ws, quien in list(clientes.items()):
        if quien == n:
            try: await ws.send_str(data)
            except: pass

async def broadcast(payload, excluir=None):
    data = json.dumps(payload)
    for ws in list(clientes.keys()):
        if ws is excluir: continue
        try: await ws.send_str(data)
        except: pass

async def usuarios_payload():
    filas = await db_lista()
    on = set(clientes.values())
    return [{"nombre":f[0],"avatar":f[1] or "","ultima":f[2] or 0,"en_linea":f[0] in on} for f in filas]

async def enviar_usuarios():
    await broadcast({"tipo":"usuarios","usuarios":await usuarios_payload(),"en_linea":len(clientes)})

async def chats_payload(n):
    filas = await db_chats(n)
    res = []
    for otro,de,texto,img,hora,ts,estado in filas:
        u = await db_usuario(otro)
        res.append({
            "otro":otro, "ultimo":{"texto":texto or "","imagen":bool(img),"hora":hora or "","ts":ts or 0,"de":de,"estado":estado or "enviado"},
            "no_leidos":no_leidos.get(n,{}).get(otro,0), "en_linea":online(otro),
            "avatar":(u[4] if u else "") or "", "ultima":(u[5] if u else 0) or 0,
        })
    res.sort(key=lambda c: c["ultimo"]["ts"], reverse=True)
    return res

async def enviar_chats(n): await enviar_a(n, {"tipo":"chats","chats":await chats_payload(n)})
async def notificar_perfil(n, bio, avatar):
    for fila in await db_chats(n): await enviar_a(fila[0], {"tipo":"perfil_actualizado_otro","nombre":n,"bio":bio,"avatar":avatar})
    await enviar_usuarios()

async def verificar_url_s3(url):
    try:
        async with aiohttp.ClientSession() as ses:
            async with ses.head(url, timeout=aiohttp.ClientTimeout(total=3)) as r: return r.status == 200
    except Exception: return False

async def ws_handler(request):
    ws = web.WebSocketResponse(max_msg_size=8_000_000); await ws.prepare(request)
    nombre = None; autenticado = False
    try:
        async for msg in ws:
            if msg.type != WSMsgType.TEXT: continue
            try: m = json.loads(msg.data)
            except: continue
            tipo = m.get("tipo")

            if tipo == "registro":
                n = (m.get("nombre") or "").strip()[:20]; p = m.get("password") or ""
                if not n or len(p) < 4: await ws.send_str(json.dumps({"tipo":"error","texto":"Nombre y contraseña (mín. 4)"})); continue
                if await db_usuario(n): await ws.send_str(json.dumps({"tipo":"error","texto":"Nombre ya registrado"})); continue
                salt, h = hash_pw(p); await db_crear_usuario(n, salt, h)
                token = secrets.token_urlsafe(24); await db_sesion_crear(token, n)
                await ws.send_str(json.dumps({"tipo":"auth_ok","token":token,"nombre":n})); continue

            if tipo == "login":
                n = (m.get("nombre") or "").strip()[:20]; p = m.get("password") or ""; u = await db_usuario(n)
                if not u or not verificar_pw(p, u[1], u[2]): await ws.send_str(json.dumps({"tipo":"error","texto":"Credenciales incorrectas"})); continue
                token = secrets.token_urlsafe(24); await db_sesion_crear(token, n)
                await ws.send_str(json.dumps({"tipo":"auth_ok","token":token,"nombre":n})); continue

            if tipo == "validar_sesion":
                n = await db_sesion(m.get("token") or "")
                if not n: await ws.send_str(json.dumps({"tipo":"sesion_invalida"})); continue
                await ws.send_str(json.dumps({"tipo":"sesion_valida","nombre":n})); continue

            if tipo == "entrar":
                n = await db_sesion(m.get("token") or "")
                if not n: await ws.send_str(json.dumps({"tipo":"sesion_invalida"})); continue
                nombre = n; autenticado = True
                for w, quien in list(clientes.items()):
                    if quien == nombre:
                        try: await w.close()
                        except: pass
                        del clientes[w]
                clientes[ws] = nombre; no_leidos.setdefault(nombre, {})
                await enviar_chats(nombre)
                for fila in await db_chats(nombre): await enviar_a(fila[0], {"tipo":"presencia","usuario":nombre,"en_linea":True,"ultima":time.time()})
                await enviar_usuarios(); continue

            if not autenticado: await ws.send_str(json.dumps({"tipo":"error","texto":"No autenticado"})); continue

            if tipo == "pedir_usuarios": await ws.send_str(json.dumps({"tipo":"usuarios","usuarios":await usuarios_payload(),"en_linea":len(clientes)})); continue
            if tipo == "pedir_chats": await enviar_chats(nombre); continue

            if tipo == "abrir_chat":
                otro = m.get("otro")
                if not otro: continue
                if nombre in no_leidos: no_leidos[nombre][otro] = 0
                ids = await db_no_vistos(otro, nombre)
                if ids:
                    await db_estado(ids, "visto"); await enviar_a(otro, {"tipo":"estados","ids":ids,"estado":"visto"})
                conv = await db_conv(nombre, otro)
                mensajes = []
                for r in conv:
                    mensajes.append({"id":r[0],"de":r[1],"para":r[2],"texto":r[3] or "","imagen_url":r[4] or "","imagen_tamano":r[5] or "","imagen_dim":r[6] or "","estado":r[7] or "enviado","hora":r[8] or "","ts":r[9] or 0,"cita":{"autor":r[10],"texto":r[11]} if r[10] else None})
                u = await db_usuario(otro)
                await ws.send_str(json.dumps({"tipo":"conversacion","otro":otro,"mensajes":mensajes,"en_linea":online(otro),"ultima":(u[5] if u else 0) or 0}))
                await enviar_chats(nombre); continue

            if tipo == "marcar_leido":
                otro = m.get("otro")
                if nombre in no_leidos and otro: no_leidos[nombre][otro] = 0
                ids = await db_no_vistos(otro, nombre)
                if ids:
                    await db_estado(ids, "visto"); await enviar_a(otro, {"tipo":"estados","ids":ids,"estado":"visto"})
                await enviar_chats(nombre); continue

            if tipo == "mensaje":
                para = m.get("para"); texto = (m.get("texto") or "").strip()
                imagen_url = m.get("imagen_url"); imagen_tamano = m.get("imagen_tamano") or ""; imagen_dim = m.get("imagen_dim") or ""
                resp = m.get("responder_a")
                if not para or (not texto and not imagen_url): continue
                if imagen_url:
                    info = urls_preparadas.get(imagen_url)
                    if not info or info[0] != nombre or time.time() - info[1] > 600: await ws.send_str(json.dumps({"tipo":"error","texto":"Imagen no autorizada o expirada"})); continue
                    if not await verificar_url_s3(imagen_url): await ws.send_str(json.dumps({"tipo":"error","texto":"La imagen no está disponible en S3"})); continue
                    urls_preparadas.pop(imagen_url, None)
                cita_autor = cita_texto = None
                if resp:
                    async with pool.connection() as conn:
                        async with conn.cursor() as cur:
                            await cur.execute("SELECT de,texto FROM mensajes WHERE id=%s", (resp,))
                            r = await cur.fetchone()
                            if r: cita_autor, cita_texto = r[0], (r[1] or "[imagen]")[:120]
                out = {"id":str(time.time_ns()),"de":nombre,"para":para,"texto":texto[:2000],"imagen_url":imagen_url or "","imagen_tamano":imagen_tamano,"imagen_dim":imagen_dim,"hora":time.strftime("%H:%M"),"ts":time.time(),"estado":"enviado","cita_autor":cita_autor,"cita_texto":cita_texto}
                if online(para): out["estado"] = "entregado"
                await db_insertar_msg(out)
                salida = dict(out)
                if cita_autor: salida["cita"] = {"autor":cita_autor,"texto":cita_texto}
                await enviar_a(para, {"tipo":"mensaje","mensaje":salida}); await enviar_a(nombre, {"tipo":"mensaje","mensaje":salida,"eco":True})
                no_leidos.setdefault(para, {}); no_leidos[para][nombre] = no_leidos[para].get(nombre,0)+1
                await enviar_chats(nombre); await enviar_chats(para); continue

            if tipo == "eliminar_chat":
                otro = m.get("otro")
                if otro:
                    await db_ocultar_chat(nombre, otro)
                    await enviar_chats(nombre)
                continue

            if tipo == "escribiendo":
                para = m.get("para")
                if para: await enviar_a(para, {"tipo":"escribiendo","de":nombre,"activo":m.get("activo",False)})
                continue

            if tipo == "actualizar_perfil":
                nuevo = (m.get("nombre") or "").strip()[:20]; bio = (m.get("bio") or "").strip()[:120]; avatar_url = m.get("avatar_url")
                if not nuevo: await ws.send_str(json.dumps({"tipo":"error","texto":"Nombre requerido"})); continue
                u = await db_usuario(nombre); nuevo_avatar = None
                if avatar_url is not None:
                    if avatar_url == "": nuevo_avatar = ""
                    else:
                        info = urls_preparadas.get(avatar_url)
                        if not info or info[0] != nombre or time.time() - info[1] > 600: await ws.send_str(json.dumps({"tipo":"error","texto":"Avatar no autorizado o expirado"})); continue
                        if not await verificar_url_s3(avatar_url): await ws.send_str(json.dumps({"tipo":"error","texto":"El avatar no está disponible en S3"})); continue
                        nuevo_avatar = avatar_url; urls_preparadas.pop(avatar_url, None)
                if nuevo != nombre:
                    if await db_usuario(nuevo): await ws.send_str(json.dumps({"tipo":"error","texto":"Ese nombre ya existe"})); continue
                    await db_renombrar(nombre, nuevo); nombre = nuevo; clientes[ws] = nuevo
                avatar_final = nuevo_avatar if nuevo_avatar is not None else (u[4] or "")
                await db_actualizar_perfil(nombre, bio, avatar_final)
                await ws.send_str(json.dumps({"tipo":"perfil_actualizado","nombre":nombre,"bio":bio,"avatar":avatar_final}))
                await notificar_perfil(nombre, bio, avatar_final); await enviar_chats(nombre); continue

            if tipo == "pedir_perfil":
                quien = m.get("quien") or nombre; u = await db_usuario(quien)
                if not u: await ws.send_str(json.dumps({"tipo":"error","texto":"Usuario no encontrado"})); continue
                await ws.send_str(json.dumps({"tipo":"perfil","nombre":u[0],"bio":u[3] or "","avatar":u[4] or "","en_linea":online(quien),"ultima":u[5] or 0})); continue

    except Exception as e: print("WS error:", e)
    finally:
        if ws in clientes:
            del clientes[ws]
            if nombre:
                ts = time.time(); await db_ultima(nombre, ts)
                for fila in await db_chats(nombre): await enviar_a(fila[0], {"tipo":"presencia","usuario":nombre,"en_linea":False,"ultima":ts})
                await enviar_usuarios()
    return ws

async def index(request): return web.FileResponse("auth.html")
async def auth(request): return web.FileResponse("auth.html")
async def chat(request): return web.FileResponse("chat.html")
async def health(request): return web.Response(text="ok")

async def upload_prepare(request):
    try: body = await request.json()
    except: return web.json_response({"error": "JSON inválido"}, status=400)
    token = body.get("token") or ""; tipo_archivo = body.get("tipo_archivo", "mensaje")
    mime = (body.get("mime") or "image/jpeg").lower(); bytes_ = int(body.get("bytes") or 0); para = body.get("para")
    n = await db_sesion(token)
    if not n: return web.json_response({"error": "No autenticado"}, status=401)
    if tipo_archivo not in ("mensaje", "avatar"): return web.json_response({"error": "tipo_archivo inválido"}, status=400)
    if tipo_archivo == "mensaje":
        if not para: return web.json_response({"error": "falta para"}, status=400)
        if bytes_ <= 0 or bytes_ > 4*1024*1024: return web.json_response({"error": "imagen fuera de rango (1 B – 4 MB)"}, status=413)
    else:
        if bytes_ <= 0 or bytes_ > 300*1024: return web.json_response({"error": "avatar fuera de rango (1 B – 300 KB)"}, status=413)
    ext = mime.split("/")[1].split("+")[0] if "/" in mime else "jpg"
    prefijo = "img" if tipo_archivo == "mensaje" else "avatar"
    key = f"{prefijo}/{uuid.uuid4().hex}.{ext}"
    url_publica = f"{S3_PUBLIC_URL}/{key}"
    urls_preparadas[url_publica] = (n, time.time())
    return web.json_response({"upload_id": uuid.uuid4().hex,"url_put": url_publica,"url_publica": url_publica,"msg_id": str(time.time_ns()) if tipo_archivo == "mensaje" else "","expira_en": 120})

async def limpiar_urls_loop(app):
    while True:
        await asyncio.sleep(60); ahora = time.time()
        for u in [u for u,(n,t) in urls_preparadas.items() if ahora - t > 600]: urls_preparadas.pop(u, None)

async def urls_cleanup_ctx(app):
    task = asyncio.create_task(limpiar_urls_loop(app)); yield; task.cancel()

async def main():
    await init_db()
    app = web.Application(client_max_size=8_000_000)
    app.router.add_get("/", index); app.router.add_get("/auth.html", auth); app.router.add_get("/chat.html", chat)
    app.router.add_get("/healthz", health); app.router.add_post("/upload/prepare", upload_prepare); app.router.add_get("/ws", ws_handler)
    app.cleanup_ctx.append(urls_cleanup_ctx)
    runner = web.AppRunner(app); await runner.setup()
    site = web.TCPSite(runner, "0.0.0.0", PORT); await site.start()
    print(f"Servidor en puerto {PORT}"); await asyncio.Event().wait()

if __name__ == "__main__": asyncio.run(main())
