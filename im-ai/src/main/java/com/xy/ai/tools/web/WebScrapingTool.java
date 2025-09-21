package com.xy.ai.tools.web;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;


/**
 * 网页抓取工具类
 */
public class WebScrapingTool {

    @Tool(description = "Scrape the content of a web page")
    public String scrapeWebPage(@ToolParam(description = "URL of the web page to scrape") String url) {
        try {
            Document doc = Jsoup.connect(url).get();  // 文档对象
            return doc.html();  // 得到完整的网页内容（String 类型的字符串）
        } catch (Exception e) {
            return "Error scraping web page: " + e.getMessage();
        }
    }
}