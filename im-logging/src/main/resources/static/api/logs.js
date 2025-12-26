import request from '../utils/request.js';

/**
 * 获取服务列表
 * @param {Object} params { env: string }
 */
export function getServices(params) {
    return request({
        url: '/api/logs/meta/services',
        method: 'get',
        params
    });
}

/**
 * 查询日志列表
 * @param {Object} params
 */
export function searchLogs(params) {
    return request({
        url: '/api/logs',
        method: 'get',
        params
    });
}

/**
 * 发送测试日志
 * @param {Object} data
 */
export function sendTestLog(data) {
    return request({
        url: '/api/logs',
        method: 'post',
        data
    });
}

/**
 * 导出日志 (通常是直接打开链接，这里提供生成 URL 的辅助，或者如果是 POST 下载流也可以封装)
 * 前端直接 window.open GET 请求即可，或者使用 blob 下载
 * 这里保留原逻辑，但在 API 层提供参数构造帮助
 */
export function getExportUrl(params) {
    const searchParams = new URLSearchParams();
    Object.keys(params).forEach(key => {
        if (params[key] !== undefined && params[key] !== null && params[key] !== '') {
            searchParams.append(key, params[key]);
        }
    });
    return `/api/logs/export?${searchParams.toString()}`;
}

export function deleteBefore(params) {
    return request({
        url: '/api/logs/before',
        method: 'delete',
        params
    });
}

export function deleteModuleBefore({module, cutoff}) {
    return request({
        url: `/api/logs/module/${encodeURIComponent(module)}/before`,
        method: 'delete',
        params: {cutoff}
    });
}

export function getOverview() {
    return request({
        url: '/api/logs/stats/overview',
        method: 'get'
    });
}

export function getHourly(params) {
    return request({
        url: '/api/logs/stats/hourly',
        method: 'get',
        params
    });
}

export function getHistogram(params) {
    return request({
        url: '/api/logs/stats/histogram',
        method: 'get',
        params
    });
}

export function getAggTopServices(params) {
    return request({
        url: '/api/logs/aggs/top/services',
        method: 'get',
        params
    });
}

export function getAggTopAddresses(params) {
    return request({
        url: '/api/logs/aggs/top/addresses',
        method: 'get',
        params
    });
}

export function getAggTopErrors(params) {
    return request({
        url: '/api/logs/aggs/top/errors',
        method: 'get',
        params
    });
}
