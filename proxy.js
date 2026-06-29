const http = require('http');
const net = require('net');

const BACKEND_URL = 'http://127.0.0.1:8081';
const FRONTEND_URL = 'http://127.0.0.1:5173';

const server = http.createServer((req, res) => {
    const isBackend = req.url.startsWith('/api/') || req.url.startsWith('/oauth2/') || req.url.startsWith('/login');
    const targetBase = isBackend ? BACKEND_URL : FRONTEND_URL;
    const targetUrl = new URL(req.url, targetBase);

    const proxyReq = http.request(targetUrl, {
        method: req.method,
        headers: req.headers
    }, (proxyRes) => {
        res.writeHead(proxyRes.statusCode, proxyRes.headers);
        proxyRes.pipe(res, { end: true });
    });

    proxyReq.on('error', (err) => {
        res.writeHead(502, { 'Content-Type': 'text/plain' });
        res.end('502 Bad Gateway: ' + err.message);
    });

    req.pipe(proxyReq, { end: true });
});

// Support WebSocket forwarding for Vite Hot Module Replacement (HMR)
server.on('upgrade', (req, socket, head) => {
    const isBackend = req.url.startsWith('/api/') || req.url.startsWith('/oauth2/') || req.url.startsWith('/login');
    const port = isBackend ? 8081 : 5173;
    
    const targetSocket = net.connect(port, '127.0.0.1', () => {
        targetSocket.write(`${req.method} ${req.url} HTTP/${req.httpVersion}\r\n`);
        for (const [key, value] of Object.entries(req.headers)) {
            targetSocket.write(`${key}: ${value}\r\n`);
        }
        targetSocket.write('\r\n');
        targetSocket.write(head);
        
        socket.pipe(targetSocket).pipe(socket);
    });
    
    targetSocket.on('error', () => {
        socket.destroy();
    });
});

server.listen(8080, () => {
    console.log('Native Reverse Proxy listening on http://localhost:8080');
});
