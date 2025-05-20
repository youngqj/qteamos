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
 * 系统启动横幅
 * 在系统启动时显示版权信息和系统版本
 *
 * @author yangqijun
 * @date 2025-05-04
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.core.system.initialization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 系统启动横幅
 * 负责在系统启动时输出版权信息和系统版本
 */
@Component
public class SystemBanner {
    private static final Logger log = LoggerFactory.getLogger(SystemBanner.class);
    
    /**
     * 当前版本号
     */
    @Value("${qteamos.version:1.0.0}")
    private String version;
    
    /**
     * 是否显示横幅
     */
    @Value("${qteamos.banner.enabled:true}")
    private boolean bannerEnabled;
    
    /**
     * 显示系统横幅
     */
    public void showBanner() {
        if (!bannerEnabled) {
            return;
        }
        
        String banner = generateBanner();
        System.out.println(banner);
        log.info("QTeamOS 版本 {} 正在启动...", version);
    }
    
    /**
     * 生成横幅文本
     *
     * @return 横幅文本
     */
    private String generateBanner() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("  $$$$$$\\    $$$$$$$$\\                                   $$$$$$\\   $$$$$$\\  \n");
        sb.append(" $$  __$$\\   \\__$$  __|                                 $$  __$$\\ $$  __$$\\ \n");
        sb.append(" $$ /  \\__|     $$ | $$$$$$\\   $$$$$$\\  $$$$$$\\$$$$\\   $$ /  $$ |$$ /  \\__|\n");
        sb.append(" $$ |          $$ |$$  __$$\\ $$  __$$\\ $$  _$$  _$$\\  $$ |  $$ |\\$$$$$$\\  \n");
        sb.append(" $$ |  $$\\     $$ |$$$$$$$$ |$$ /  $$ |$$ / $$ / $$ |  $$ |  $$ | \\____$$\\ \n");
        sb.append(" $$ |\\$$  |    $$ |$$   ____|$$ |  $$ |$$ | $$ | $$ |  $$ |  $$ |$$\\   $$ |\n");
        sb.append(" \\$$$$$$  /    $$ |\\$$$$$$$\\ \\$$$$$$  |$$ | $$ | $$ |   $$$$$$  |\\$$$$$$  |\n");
        sb.append("  \\______/     \\__| \\_______| \\______/ \\__| \\__| \\__|   \\______/  \\______/ \n");
        sb.append("\n");
        sb.append("===================================================================\n");
        sb.append(":: QTeamOS ::                                         (v" + version + ")\n");
        sb.append("===================================================================\n");
        sb.append("Copyright (c) 2023-2025 XiaoQuTeam. All rights reserved.\n");
        sb.append("QTeamOS is licensed under Mulan PSL v2.\n");
        sb.append("===================================================================\n");
        
        return sb.toString();
    }
    
    /**
     * 显示系统启动完成信息
     */
    public void showStartupCompleted() {
        log.info("QTeamOS 启动完成! 版本: {}", version);
        if (bannerEnabled) {
            System.out.println("===================================================================");
            System.out.println(":: QTeamOS 启动完成 ::                              (v" + version + ")");
            System.out.println("===================================================================");
        }
    }
} 