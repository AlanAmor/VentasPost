const ApiClient = {
    async request(url, options = {}) {
        const config = {
            credentials: 'same-origin',
            headers: {
                'Content-Type': 'application/json',
                ...(options.headers || {})
            },
            ...options
        };

        const response = await fetch(url, config);
        let data = null;
        const contentType = response.headers.get('content-type') || '';
        if (contentType.includes('application/json')) {
            data = await response.json();
        }

        if (!response.ok) {
            const error = new Error(this.extractErrorMessage(data, response.status));
            error.status = response.status;
            error.payload = data;
            throw error;
        }

        return data;
    },

    extractErrorMessage(data, status) {
        if (!data) {
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
    }
};
