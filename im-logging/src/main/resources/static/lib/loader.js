const {loadModule} = window['vue3-sfc-loader'];

export const options = {
    moduleCache: {
        vue: Vue,
        "element-plus": ElementPlus,
        less: less
    },
    async getFile(url) {
        const res = await fetch(url);
        if (!res.ok) throw Object.assign(new Error(res.statusText + " " + url), {res});

        // Treat .js files as ES modules (.mjs) so that import/export statements work
        const type = url.endsWith('.js') ? '.mjs' : undefined;

        return {
            getContentData: asBinary => asBinary ? res.arrayBuffer() : res.text(),
            type
        };
    },
    addStyle(textContent) {
        const style = Object.assign(document.createElement("style"), {textContent});
        const ref = document.head.getElementsByTagName("style")[0] || null;
        document.head.insertBefore(style, ref);
    },
    // Ensure relative paths are resolved correctly
    handleModule: async function (type, getContentData, path, options) {
        switch (type) {
            case '.css':
                options.addStyle(await getContentData(false));
                return null;
            case '.less':
                // Implement less support if needed, or rely on default if 'less' is in moduleCache
                // default handleModule usually handles .less if less is provided
                // returning undefined lets the default handler take over
                return undefined;
            default:
                return undefined;
        }
    }
};

export const load = (path) => loadModule(path, options);
