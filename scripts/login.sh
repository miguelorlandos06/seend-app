#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

read -r -p "Usuario: " NOMBRE
read -r -s -p "Contraseña: " PASS; echo

TOKEN=$(python3 -c '
import asyncio, json, sys, websockets
nombre, password = sys.argv[1], sys.argv[2]
async def main():
    async with websockets.connect("wss://seend-i33g.onrender.com/ws") as ws:
        await ws.send(json.dumps({"tipo": "login", "nombre": nombre, "password": password}))
        while True:
            r = json.loads(await ws.recv())
            if r.get("tipo") == "auth_ok":
                print(r["token"]); return
            if r.get("tipo") == "error":
                print("ERROR:" + r.get("texto", ""), file=sys.stderr); sys.exit(1)
asyncio.run(main())
' "$NOMBRE" "$PASS")

echo "$TOKEN" > ~/.seend_token
chmod 600 ~/.seend_token
echo "✅ Token guardado en ~/.seend_token"
