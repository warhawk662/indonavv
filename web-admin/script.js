const canvas = document.getElementById('mapCanvas');
const ctx = canvas.getContext('2d');
const bgUpload = document.getElementById('bgUpload');
const statusText = document.getElementById('statusText');
const poiEditor = document.getElementById('poiEditor');

let nodes = [];
let edges = [];
let pois = [];
let geofences = [];
let bgImage = null;

let selectedNode = null;
let firstNodeForEdge = null;
let currentGeofencePoints = [];

// Determine API Base
let API_BASE = "http://localhost:8080";
if (window.location.hostname !== "localhost" && window.location.hostname !== "127.0.0.1") {
    API_BASE = `http://${window.location.hostname}:8080`;
}

window.onload = () => {
    resizeCanvas();
    loadMap();
};

function resizeCanvas() {
    if (!bgImage) {
        const container = canvas.parentElement;
        canvas.width = container.clientWidth || 1000;
        canvas.height = container.clientHeight || 800;
    }
    draw();
}

// 1. IMPROVED UPLOAD with Local Preview
bgUpload.addEventListener('change', (e) => {
    const file = e.target.files[0];
    if (!file) return;

    // LOCAL PREVIEW: Show on canvas immediately
    const reader = new FileReader();
    reader.onload = (event) => {
        const img = new Image();
        img.onload = () => {
            bgImage = img;
            canvas.width = img.naturalWidth;
            canvas.height = img.naturalHeight;
            draw();
            statusText.innerText = "Local preview loaded. Uploading to server...";
            uploadImage(file);
        };
        img.src = event.target.result;
    };
    reader.readAsDataURL(file);
});

async function uploadImage(file) {
    const formData = new FormData();
    formData.append('file', file);
    try {
        const response = await fetch(`${API_BASE}/map/image`, { method: 'POST', body: formData });
        if (response.ok) {
            statusText.innerText = "Map saved to server.";
        } else {
            statusText.innerText = "Upload failed, but local preview is active.";
        }
    } catch (err) {
        console.error("Upload error:", err);
        statusText.innerText = "Connection error. Local preview only.";
    }
}

function loadBackgroundImage(url) {
    const img = new Image();
    img.crossOrigin = "anonymous";
    img.onload = () => {
        bgImage = img;
        canvas.width = img.naturalWidth;
        canvas.height = img.naturalHeight;
        draw();
    };
    img.src = url + (url.includes('?') ? '&' : '?') + 't=' + new Date().getTime();
}

canvas.addEventListener('mousedown', (e) => {
    const rect = canvas.getBoundingClientRect();
    const scaleX = canvas.width / rect.width;
    const scaleY = canvas.height / rect.height;
    const x = Math.round((e.clientX - rect.left) * scaleX);
    const y = Math.round((e.clientY - rect.top) * scaleY);

    const mode = document.querySelector('input[name="mode"]:checked').id;
    if (mode === "modeGeofence") {
        currentGeofencePoints.push({ x, y });
        draw();
    } else {
        statusText.innerHTML = `X: ${x}, Y: ${y}`;
        if (mode === "modeNode") addNode(x, y);
        else if (mode === "modeEdge") handleEdgeClick(x, y);
        else if (mode === "modePoi") handlePoiClick(x, y);
    }
});

function draw() {
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    if (bgImage) ctx.drawImage(bgImage, 0, 0);
    else {
        ctx.fillStyle = "#eee"; ctx.fillRect(0, 0, canvas.width, canvas.height);
        ctx.fillStyle = "#666"; ctx.fillText("Upload Map Image", canvas.width/2 - 50, canvas.height/2);
    }

    geofences.forEach(gf => {
        if (gf.points.length < 2) return;
        ctx.fillStyle = "rgba(255, 0, 0, 0.2)"; ctx.strokeStyle = "red";
        ctx.beginPath(); ctx.moveTo(gf.points[0].x, gf.points[0].y);
        gf.points.forEach(p => ctx.lineTo(p.x, p.y));
        ctx.closePath(); ctx.fill(); ctx.stroke();
    });

    if (currentGeofencePoints.length > 0) {
        ctx.strokeStyle = "#007bff"; ctx.beginPath();
        ctx.moveTo(currentGeofencePoints[0].x, currentGeofencePoints[0].y);
        currentGeofencePoints.forEach(p => ctx.lineTo(p.x, p.y));
        ctx.stroke();
        currentGeofencePoints.forEach(p => {
            ctx.fillStyle = "yellow"; ctx.beginPath(); ctx.arc(p.x, p.y, 5, 0, Math.PI*2); ctx.fill();
        });
    }

    ctx.strokeStyle = "#ff4081"; ctx.lineWidth = 4;
    edges.forEach(edge => {
        const from = nodes.find(n => n.id === edge.fromNodeId);
        const to = nodes.find(n => n.id === edge.toNodeId);
        if (from && to) { ctx.beginPath(); ctx.moveTo(from.x, from.y); ctx.lineTo(to.x, to.y); ctx.stroke(); }
    });

    nodes.forEach(node => {
        const isSelected = (node === selectedNode || node === firstNodeForEdge);
        ctx.fillStyle = isSelected ? "#ffc107" : "#007bff";
        ctx.beginPath(); ctx.arc(node.x, node.y, 8, 0, Math.PI * 2); ctx.fill();
        const poi = pois.find(p => p.nodeId === node.id);
        if (poi) { ctx.fillStyle = "#198754"; ctx.font = "bold 12px Arial"; ctx.fillText(poi.name, node.x + 12, node.y + 4); }
    });
}

async function loadMap() {
    try {
        const response = await fetch(`${API_BASE}/map`);
        const data = await response.json();
        nodes = data.nodes || []; edges = data.edges || []; pois = data.pois || []; geofences = data.geofences || [];
        if (data.bgImageUrl) loadBackgroundImage(`${API_BASE}/${data.bgImageUrl}`);
        draw();
    } catch (err) { console.error(err); }
}

async function finishPolygon() {
    if (currentGeofencePoints.length < 3) return;
    const gf = { id: "gf" + Date.now(), points: [...currentGeofencePoints] };
    const res = await fetch(`${API_BASE}/map/geofence`, {
        method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(gf)
    });
    if (res.ok) { geofences.push(gf); currentGeofencePoints = []; draw(); }
}

async function addNode(x, y) {
    const node = { id: "n" + Date.now(), x, y, floorId: "lg", type: "JUNCTION" };
    const res = await fetch(`${API_BASE}/map/node`, {
        method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(node)
    });
    if (res.ok) { nodes.push(node); draw(); }
}

function handleEdgeClick(x, y) {
    const node = nodes.find(n => Math.sqrt(Math.pow(n.x - x, 2) + Math.pow(n.y - y, 2)) < 20);
    if (!node) return;
    if (!firstNodeForEdge) firstNodeForEdge = node;
    else {
        if (firstNodeForEdge.id !== node.id) addEdge(firstNodeForEdge.id, node.id);
        firstNodeForEdge = null;
    }
    draw();
}

async function addEdge(fromId, toId) {
    const edge = { id: "e" + Date.now(), fromNodeId: fromId, toNodeId: toId, distance: 1.0, weight: 1.0 };
    const res = await fetch(`${API_BASE}/map/edge`, {
        method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(edge)
    });
    if (res.ok) { edges.push(edge); draw(); }
}

function handlePoiClick(x, y) {
    const node = nodes.find(n => Math.sqrt(Math.pow(n.x - x, 2) + Math.pow(n.y - y, 2)) < 20);
    if (node) {
        selectedNode = node;
        document.getElementById('poiNodeId').value = node.id;
        const p = pois.find(p => p.nodeId === node.id);
        document.getElementById('poiName').value = p ? p.name : "";
        poiEditor.classList.remove('d-none');
    } else { selectedNode = null; poiEditor.classList.add('d-none'); }
    draw();
}

async function savePOI() {
    const poi = { id: "p" + Date.now(), name: document.getElementById('poiName').value, nodeId: document.getElementById('poiNodeId').value, category: "Room" };
    const res = await fetch(`${API_BASE}/map/poi`, {
        method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(poi)
    });
    if (res.ok) { pois = pois.filter(p => p.nodeId !== poi.nodeId); pois.push(poi); poiEditor.classList.add('d-none'); draw(); }
}

function undo() { if (currentGeofencePoints.length > 0) { currentGeofencePoints.pop(); draw(); } }

async function clearMap() {
    if (!confirm("Clear all?")) return;
    await fetch(`${API_BASE}/map/clear`, { method: "DELETE" });
    nodes = []; edges = []; pois = []; geofences = []; currentGeofencePoints = []; draw();
}
