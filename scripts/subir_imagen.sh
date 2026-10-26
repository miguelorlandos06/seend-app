#!/data/data/com.termux/files/usr/bin/bash
# ------------------------------------------------------------
# Simula la subida de una imagen como lo hace chat.html
# Uso: ./subir_imagen.sh <ruta_imagen> <destinatario> [texto]
# Ejemplo: ./subir_imagen.sh ~/foto.jpg ana "Mira esto"
# ------------------------------------------------------------
set -euo pipefail

HOST="seend-i33g.onrender.com"
BASE="https://$HOST"

IMAGEN="${1:-}"
PARA="${2:-}"
TEXTO="${3:-}"

if [ -z "$IMAGEN" ] || [ -z "$PARA" ]; then
  echo "Uso: $0 <ruta_imagen> <destinatario> [texto]"
  exit 1
fi

if [ ! -f "$IMAGEN" ]; then
  echo "❌ No existe el archivo: $IMAGEN"
  exit 1
fi

echo "🔎 Detectando MIME..."
MIME=$(file --mime-type -b "$IMAGEN")
BYTES=$(wc -c < "$IMAGEN")
TAMANO_LEGIBLE=$(python3 -c "b=$BYTES; print(f'{b} B' if b<1024 else f'{b/1024:.1f} KB' if b<1048576 else f'{b/1048576:.1f} MB')")

echo "   MIME:  $MIME"
echo "   Bytes: $BYTES ($TAMANO_LEGIBLE)"

# Dimensiones con Python (requiere Pillow opcional; si no, vacío)
DIM=""
if python3 -c "import PIL" 2>/dev/null; then
  DIM=$(python3 - <<'PY'
from PIL import Image
import sys
try:
    im = Image.open(sys.argv[1]) if len(sys.argv)>1 else None
except Exception:
    im = None
print("")
PY
"$IMAGEN")
  DIM=$(python3 -c "
from PIL import Image
try:
    im = Image.open('$IMAGEN')
    print(f'{im.width} × {im.height}')
except Exception:
    print('')
")
fi
echo "   Dim:   ${DIM:-'(no detectadas, instala pillow: pip install pillow)'}"

# Token: léelo de ~/.seend_token o pídelo
TOKEN_FILE="$HOME/.seend_token"
if [ -f "$TOKEN_FILE" ]; then
  TOKEN=$(cat "$TOKEN_FILE")
else
  read -r -p "🔑 Token (lo tienes en la web, localStorage.token): " TOKEN
  echo "$TOKEN" > "$TOKEN_FILE"
  chmod 600 "$TOKEN_FILE"
fi

echo ""
echo "📤 1/3 Preparando subida..."
PREP=$(curl -sS -X POST "$BASE/upload/prepare" \
  -H "Content-Type: application/json" \
  -d "$(jq -n \
    --arg token "$TOKEN" \
    --arg tipo "mensaje" \
    --arg mime "$MIME" \
    --argjson bytes "$BYTES" \
    --arg para "$PARA" \
    '{token:$token, tipo_archivo:$tipo, mime:$mime, bytes:$bytes, para:$para}')")

echo "$PREP" | jq .

URL_PUT=$(echo "$PREP" | jq -r '.url_put // empty')
URL_PUB=$(echo "$PREP" | jq -r '.url_publica // empty')
ERROR=$(echo "$PREP" | jq -r '.error // empty')

if [ -n "$ERROR" ]; then
  echo "❌ Error en /upload/prepare: $ERROR"
  exit 1
fi
if [ -z "$URL_PUT" ] || [ "$URL_PUT" = "null" ]; then
  echo "❌ Respuesta inválida del servidor"
  exit 1
fi

echo ""
echo "📤 2/3 Subiendo a S3..."
echo "   PUT $URL_PUT"
HTTP_CODE=$(curl -sS -o /dev/null -w "%{http_code}" -X PUT "$URL_PUT" \
  -H "Content-Type: $MIME" \
  --data-binary "@$IMAGEN")

if [ "$HTTP_CODE" -lt 200 ] || [ "$HTTP_CODE" -ge 300 ]; then
  echo "❌ PUT falló (HTTP $HTTP_CODE)"
  exit 1
fi
echo "   ✅ HTTP $HTTP_CODE"

echo ""
echo "📤 3/3 Enviando mensaje por WebSocket..."
python3 - "$TOKEN" "$PARA" "$URL_PUB" "$TAMANO_LEGIBLE" "$DIM" "$TEXTO" <<'PY'
import asyncio, json, sys, websockets

token, para, url_pub, tamano, dim, texto = sys.argv[1:7]

async def main():
    uri = "wss://seend-i33g.onrender.com/ws"
    async with websockets.connect(uri, max_size=8_000_000) as ws:
        await ws.send(json.dumps({"tipo": "validar_sesion", "token": token}))
        r = json.loads(await ws.recv())
        if r.get("tipo") != "sesion_valida":
            print("❌ Token inválido:", r)
            return
        await ws.send(json.dumps({"tipo": "entrar", "token": token}))
        # consumimos lo que llegue (chats, presencia, etc.) hasta "sesion_valida" o 2 s
        try:
            while True:
                await asyncio.wait_for(ws.recv(), timeout=1.0)
        except asyncio.TimeoutError:
            pass

        msg = {
            "tipo": "mensaje",
            "para": para,
            "texto": texto,
            "imagen_url": url_pub,
            "imagen_tamano": tamano,
        }
        if dim:
            msg["imagen_dim"] = dim
        await ws.send(json.dumps(msg))
        print("   ✅ Mensaje enviado")
        # Esperamos eco
        try:
            while True:
                r = json.loads(await asyncio.wait_for(ws.recv(), timeout=5.0))
                if r.get("tipo") == "mensaje" and r.get("eco"):
                    print("   ✅ Eco recibido del servidor")
                    break
                if r.get("tipo") == "error":
                    print("   ❌ Error del servidor:", r.get("texto"))
                    break
        except asyncio.TimeoutError:
            pass

asyncio.run(main())
PY

echo ""
echo "🎉 Listo. Abre la web ($BASE) y verás la imagen enviada a '$PARA'."
