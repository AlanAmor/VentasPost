const ApiClient = {
    activeRequests: 0,

    setLoading(active) {
        if (active) {
            this.activeRequests++;
        } else {
            this.activeRequests = Math.max(0, this.activeRequests - 1);
        }
        const bar = document.getElementById('global-loading-bar');
        if (bar) {
            bar.classList.toggle('active', this.activeRequests > 0);
        }
    },

    async request(url, options = {}) {
        const config = {
            credentials: 'same-origin',
            headers: {
                'Content-Type': 'application/json',
                ...(options.headers || {})
            },
            ...options
        };

        this.setLoading(true);
        let response;
        try {
            response = await fetch(url, config);
        } catch (networkErr) {
            this.setLoading(false);
            const error = new Error('No se pudo conectar con el servidor. Verifique su conexión de red o que el servicio esté activo.');
            error.status = 0;
            throw error;
        }

        let data = null;
        try {
            const contentType = response.headers.get('content-type') || '';
            if (contentType.includes('application/json')) {
                data = await response.json();
            }
        } catch (parseErr) {
            data = null;
        } finally {
            this.setLoading(false);
        }

        if (!response.ok) {
            if (response.status === 401 && !url.includes('/api/v1/auth/me') && !url.includes('/api/v1/auth/login')) {
                const appView = document.getElementById('view-app');
                if (appView && !appView.classList.contains('hidden') && typeof mostrarVista === 'function') {
                    mostrarVista('view-login');
                }
            }
            const error = new Error(this.extractErrorMessage(data, response.status));
            error.status = response.status;
            error.payload = data;
            throw error;
        }

        return data;
    },

    extractErrorMessage(data, status) {
        if (!data) {
            if (status === 403) return 'No cuenta con los permisos necesarios para realizar esta operación.';
            if (status === 404) return 'El recurso solicitado no fue encontrado.';
            if (status >= 500) return 'Error interno del servidor. Intente nuevamente en unos instantes.';
            return `Error HTTP ${status}`;
        }
        if (data.validationErrors) {
            return Object.values(data.validationErrors).join(' | ');
        }
        return data.message || `Error HTTP ${status}`;
    },

    get(url) {
        return this.request(url, { method: 'GET' });
    },

    post(url, body) {
        return this.request(url, {
            method: 'POST',
            body: JSON.stringify(body || {})
        });
    },

    put(url, body) {
        return this.request(url, {
            method: 'PUT',
            body: JSON.stringify(body || {})
        });
    },

    patch(url, body) {
        return this.request(url, {
            method: 'PATCH',
            body: JSON.stringify(body || {})
        });
    },

    delete(url) {
        return this.request(url, { method: 'DELETE' });
    }
};
