"""로컬 개발용 정적 서버: python serve.py  (기본 http://localhost:5500)

Windows에서는 레지스트리 설정 때문에 python -m http.server가 .js를 text/plain으로 보내
ES 모듈(<script type="module">)이 로드되지 않는다. 여기서 MIME 타입을 바로잡는다.
"""
import functools
import http.server
import pathlib
import sys

PORT = int(sys.argv[1]) if len(sys.argv) > 1 else 5500
ROOT = pathlib.Path(__file__).resolve().parent


class Handler(http.server.SimpleHTTPRequestHandler):
    extensions_map = {
        **http.server.SimpleHTTPRequestHandler.extensions_map,
        ".js": "text/javascript",
        ".css": "text/css",
        ".html": "text/html",
        ".svg": "image/svg+xml",
        ".png": "image/png",
        ".json": "application/json",
    }

    def end_headers(self):
        # 개발 중에는 수정한 파일이 바로 반영되도록 캐시하지 않는다.
        self.send_header("Cache-Control", "no-store")
        super().end_headers()


if __name__ == "__main__":
    handler = functools.partial(Handler, directory=str(ROOT))
    with http.server.ThreadingHTTPServer(("127.0.0.1", PORT), handler) as server:
        print(f"EnjoyTrip frontend: http://localhost:{PORT}")
        server.serve_forever()
