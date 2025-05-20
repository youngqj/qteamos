/*
 * Copyright (c) 2023-2025 XiaoQuTeam. All rights reserved.
 * QTeamOS is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *          http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
 * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
 * MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

/**
 * 视图演示控制器
 * 提供简单的视图页面展示
 *
 * @author yangqijun
 * @date 2023-05-15
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.plugin.helloworld.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 视图演示控制器
 * 演示插件视图页面实现
 */
@Controller
@RequestMapping("/view")
public class ViewDemoController {
    
    private static final Logger log = LoggerFactory.getLogger(ViewDemoController.class);
    
    /**
     * 输出简单的HTML页面
     * 
     * @return HTML内容
     */
    @GetMapping(value = "/hello", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String helloHtml() {
        log.info("ViewDemoController.helloHtml方法被调用");
        
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head>");
        html.append("<meta charset=\"UTF-8\">");
        html.append("<title>Hello World</title>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; }");
        html.append(".container { max-width: 800px; margin: 0 auto; background-color: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }");
        html.append("h1 { color: #333; }");
        html.append("p { color: #666; }");
        html.append(".time { font-style: italic; color: #999; margin-top: 20px; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        html.append("<div class=\"container\">");
        html.append("<h1>Hello World!</h1>");
        html.append("<p>这是一个简单的视图示例页面。</p>");
        html.append("<p>QTeamOS插件系统视图演示。</p>");
        html.append("<p class=\"time\">当前时间: " + java.time.LocalDateTime.now() + "</p>");
        html.append("</div>");
        html.append("</body>");
        html.append("</html>");
        
        return html.toString();
    }
} 