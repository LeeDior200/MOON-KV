const API_BASE = '/api/v1';

const api = {
    async get(key) {
        const response = await fetch(`${API_BASE}/kv/${encodeURIComponent(key)}`);
        return response.json();
    },

    async set(key, value, ttl = null) {
        const body = { value };
        if (ttl !== null && ttl > 0) {
            body.ttl = ttl;
        }

        const response = await fetch(`${API_BASE}/kv/${encodeURIComponent(key)}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(body)
        });
        return response.json();
    },

    async delete(key) {
        const response = await fetch(`${API_BASE}/kv/${encodeURIComponent(key)}`, {
            method: 'DELETE'
        });
        return response.json();
    },

    async setTtl(key, ttl) {
        const response = await fetch(`${API_BASE}/kv/${encodeURIComponent(key)}/ttl`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ ttl })
        });
        return response.json();
    },

    async getAllKeys() {
        const response = await fetch(`${API_BASE}/kv`);
        return response.json();
    },

    async getStats() {
        const response = await fetch(`${API_BASE}/stats`);
        return response.json();
    },

    async getMemoryStats() {
        const response = await fetch(`${API_BASE}/stats/memory`);
        return response.json();
    },

    async getConfig() {
        const response = await fetch(`${API_BASE}/config`);
        return response.json();
    },

    async updateConfig(config) {
        const response = await fetch(`${API_BASE}/config`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(config)
        });
        return response.json();
    },

    async getHealth() {
        const response = await fetch(`${API_BASE}/health`);
        return response.json();
    },

    async getReady() {
        const response = await fetch(`${API_BASE}/health/ready`);
        return response.json();
    },

    async getLive() {
        const response = await fetch(`${API_BASE}/health/live`);
        return response.json();
    }
};

function showError(message) {
    alert('Error: ' + message);
}

function showSuccess(message) {
    console.log('Success:', message);
}

function formatBytes(bytes) {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
}

function formatUptime(ms) {
    const seconds = Math.floor(ms / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);
    const days = Math.floor(hours / 24);

    if (days > 0) {
        return `${days}d ${hours % 24}h ${minutes % 60}m`;
    } else if (hours > 0) {
        return `${hours}h ${minutes % 60}m`;
    } else if (minutes > 0) {
        return `${minutes}m ${seconds % 60}s`;
    } else {
        return `${seconds}s`;
    }
}
