package com.organizer.platform.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class GlobalJsController {
    @GetMapping(value = "/js/globals.js", produces = "application/javascript")
    @ResponseBody
    public String getGlobalsJs(Model model) {
        StringBuilder js = new StringBuilder();

        // Add categories hierarchy data
        js.append("window.categoriesHierarchyData = ")
                .append(model.getAttribute("categoriesHierarchy"))
                .append(";\n\n");

        // Add admin page data
        js.append("window.authorizedUsers = ")
                .append(model.getAttribute("authorizedUsers"))
                .append(";\n");

        js.append("window.adminUsers = ")
                .append(model.getAttribute("adminUsers"))
                .append(";\n");

        js.append("window.unauthorizedUsers = ")
                .append(model.getAttribute("unauthorizedUsers"))
                .append(";\n\n");

        // Add static mini categories data
        js.append("window.miniCategoriesData = ")
                .append("[{name:'אומנות גוף',value:2,children:[{name:'קעקועים מינימליסטיים',value:2},{name:'קעקועים יפים',value:1}]},{name:'ביוגרפיה',value:1,children:[{name:'דמות ציבורית דתית',value:1}]},{name:'דוגמה טכנולוגית',value:3,children:[{name:'תכנות בסיסי',value:5},{name:'מדריך פשוט',value:2},{name:'התקנת מחשב',value:2}]},{name:'אקדמי',value:1,children:[{name:'אישור תכנית לימודים',value:2}]}]")
                .append(";\n\n");

        // Add treemap options
        js.append("window.treeMapOptions = {")
                .append("plugins:{title:{display:true,text:'התפלגות הודעות לפי קטגוריה(דוגמא)',font:{size:18,weight:'bold'},padding:{top:10}},")
                .append("legend:{display:false},")
                .append("tooltip:{titleFont:{size:16},bodyFont:{size:16},")
                .append("callbacks:{")
                .append("title(items){if(!items[0]||!items[0].raw)return '';const item=window.miniCategoriesData[items[0].dataIndex];return item?item.name:''},")
                .append("label(context){if(!context.raw)return '';const item=window.miniCategoriesData[context.dataIndex];return `מספר הודעות: ${item.value}`}")
                .append("},")
                .append("rtl:true,textDirection:'rtl'}},")
                .append("animation:{duration:500},")
                .append("maintainAspectRatio:false")
                .append("};");

        return js.toString();
    }
}
