let refreshInterval;

async function loadDashboard() {
    try {
        await Promise.all([
            loadStats(),
            loadHealth()
        ]);
    } catch (error) {
        console.error('Failed to load dashboard:', error);
    }
}

async function loadStats() {
    try {
        const response = await api.getStats();

        if (response.code === 200) {
            const data = response.data;

            document.getElementById('keyCount').textContent = data.keyCount || 0;
            document.getElementById('memoryUsage').textContent = formatBytes(data.memoryUsage || 0);
            document.getElementById('evictionStrategy').textContent = data.evictionStrategy || 'LRU';

            if (data.jvm) {
                document.getElementById('jvmMemory').textContent =
                    `${formatBytes(data.jvm.usedMemory)} / ${formatBytes(data.jvm.maxMemory)}`;
            }
        }
    } catch (error) {
        console.error('Failed to load stats:', error);
    }
}

async function loadHealth() {
    try {
        const response = await api.getHealth();

        if (response.code === 200) {
            const data = response.data;

            document.getElementById('uptime').textContent = 'Uptime: ' + formatUptime(data.uptime);
            document.getElementById('version').textContent = 'Version: ' + data.version;

            const healthGrid = document.getElementById('healthStatus');
            healthGrid.innerHTML = '';

            if (data.components) {
                for (const [name, component] of Object.entries(data.components)) {
                    const item = document.createElement('div');
                    item.className = 'health-item';

                    if (component.status === 'WARNING') {
                        item.classList.add('warning');
                    } else if (component.status === 'DOWN') {
                        item.classList.add('danger');
                    }

                    item.innerHTML = `
                        <h4>${name}</h4>
                        <p>${component.status}</p>
                    `;

                    healthGrid.appendChild(item);
                }
            }
        }
    } catch (error) {
        console.error('Failed to load health:', error);
        document.getElementById('healthStatus').innerHTML = '<p class="error">Failed to load health status</p>';
    }
}

async function setKeyValue() {
    const key = document.getElementById('setKey').value.trim();
    const value = document.getElementById('setValue').value.trim();
    const ttl = document.getElementById('setTtl').value;

    if (!key || !value) {
        showError('Key and value are required');
        return;
    }

    try {
        const response = await api.set(key, value, ttl ? parseInt(ttl) : null);

        if (response.code === 200) {
            showSuccess('Key set successfully');
            document.getElementById('setKey').value = '';
            document.getElementById('setValue').value = '';
            document.getElementById('setTtl').value = '';
            await loadStats();
        } else {
            showError(response.message);
        }
    } catch (error) {
        showError('Failed to set key: ' + error.message);
    }
}

async function getKeyValue() {
    const key = document.getElementById('getKey').value.trim();
    const resultDiv = document.getElementById('getResult');

    if (!key) {
        showError('Key is required');
        return;
    }

    try {
        resultDiv.textContent = 'Loading...';

        const response = await api.get(key);

        if (response.code === 200) {
            resultDiv.innerHTML = `<strong>Key:</strong> ${response.data.key}<br><strong>Value:</strong> ${response.data.value}`;
        } else if (response.code === 404) {
            resultDiv.textContent = 'Key not found';
        } else {
            resultDiv.textContent = 'Error: ' + response.message;
        }
    } catch (error) {
        resultDiv.textContent = 'Error: ' + error.message;
    }
}

async function deleteKey() {
    const key = document.getElementById('deleteKey').value.trim();

    if (!key) {
        showError('Key is required');
        return;
    }

    if (!confirm(`Are you sure you want to delete key "${key}"?`)) {
        return;
    }

    try {
        const response = await api.delete(key);

        if (response.code === 200) {
            showSuccess('Key deleted successfully');
            document.getElementById('deleteKey').value = '';
            await loadStats();
        } else if (response.code === 404) {
            showError('Key not found');
        } else {
            showError(response.message);
        }
    } catch (error) {
        showError('Failed to delete key: ' + error.message);
    }
}

document.addEventListener('DOMContentLoaded', function() {
    loadDashboard();

    refreshInterval = setInterval(loadDashboard, 5000);
});

window.addEventListener('beforeunload', function() {
    if (refreshInterval) {
        clearInterval(refreshInterval);
    }
});
