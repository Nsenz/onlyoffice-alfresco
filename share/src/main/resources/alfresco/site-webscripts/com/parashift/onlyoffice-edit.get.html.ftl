<!--
    Copyright (c) Ascensio System SIA 2021. All rights reserved.
    http://www.onlyoffice.com
-->
<html>
<head>
    <meta http-equiv='Content-Type' content='text/html; charset=utf-8'>

    <title>${docTitle} - ONLYOFFICE</title>

    <link href="${url.context}/res/components/onlyoffice/onlyoffice.css" type="text/css" rel="stylesheet">

    <!--Change the address on installed ONLYOFFICE™ Online Editors-->
    <script id="scriptApi" type="text/javascript" src="${onlyofficeUrl}OfficeWeb/apps/api/documents/api.js"></script>
</head>

<body>
    <div>
        <div id="placeholder"></div>
    </div>
    <script>

    var onAppReady = function (event) {
        if (${demo?c}) {
             docEditor.showMessage("${msg("alfresco.document.onlyoffice.action.edit.msg.demo")}");
        }
    };

    var config = ${config};

    config.events = {
        "onAppReady": onAppReady
    };

    if (/Android|webOS|iPhone|iPad|iPod|BlackBerry|BB|PlayBook|IEMobile|Windows Phone|Kindle|Silk|Opera Mini/i.test(navigator.userAgent)) {
        config.type='mobile';
    }
    var docEditor = new DocsAPI.DocEditor("placeholder", config);

    </script>
</body>
</html>

