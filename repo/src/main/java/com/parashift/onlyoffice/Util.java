package com.parashift.onlyoffice;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.admin.SysAdminParams;
import org.alfresco.service.cmr.coci.CheckOutCheckInService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.UrlUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.surf.util.URLEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.*;

/*
   Copyright (c) Ascensio System SIA 2021. All rights reserved.
   http://www.onlyoffice.com
*/

@Service
public class Util {

    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

    @Autowired
    @Qualifier("checkOutCheckInService")
    CheckOutCheckInService cociService;

    @Autowired
    AuthenticationService authenticationService;

    @Autowired
    VersionService versionService;

    @Autowired
    SysAdminParams sysAdminParams;

    @Autowired
    NodeService nodeService;

    @Autowired
    ConfigManager configManager;

    @Autowired
    SearchService searchService;

    @Autowired
    NamespaceService namespaceService;

    public static final QName EditingKeyAspect = QName.createQName("onlyoffice:editing-key");
    public static final QName EditingHashAspect = QName.createQName("onlyoffice:editing-hash");
    private static final String HOME_DIRECTORY = "Company Home";

    public static final Map<String, String> PathLocale = new HashMap<String, String>(){{
        put("az", "az-Latn-AZ");
        put("bg", "bg-BG");
        put("cs", "cs-CZ");
        put("de", "de-DE");
        put("el", "el-GR");
        put("en-GB", "en-GB");
        put("en", "en-US");
        put("es", "es-ES");
        put("fr", "fr-FR");
        put("it", "it-IT");
        put("ja", "ja-JP");
        put("ko", "ko-KR");
        put("lv", "lv-LV");
        put("nl", "nl-NL");
        put("pl", "pl-PL");
        put("pt-BR", "pt-BR");
        put("pt", "pt-PT");
        put("ru", "ru-RU");
        put("sk", "sk-SK");
        put("sv", "sv-SE");
        put("uk", "uk-UA");
        put("vi", "vi-VN");
        put("zh", "zh-CN");
    }};

    public String getKey(NodeRef nodeRef) {
        String key = null;
        if (cociService.isCheckedOut(nodeRef)) {
            key = (String) nodeService.getProperty(cociService.getWorkingCopy(nodeRef), EditingKeyAspect);
        }

        if (key == null) {
            Map<QName, Serializable> properties = nodeService.getProperties(nodeRef);
            String version = (String) properties.get(ContentModel.PROP_VERSION_LABEL);

            if (version == null || version.isEmpty()) {
                ensureVersioningEnabled(nodeRef);
                Version v = versionService.getCurrentVersion(nodeRef);
                key = nodeRef.getId() + "_" + v.getVersionLabel();
            } else {
                key = nodeRef.getId() + "_" + version;
            }
        }

        return key;
    }

    public String getHash(NodeRef nodeRef) {
        String hash = null;
        if (cociService.isCheckedOut(nodeRef)) {
            hash = (String) nodeService.getProperty(cociService.getWorkingCopy(nodeRef), EditingHashAspect);
        }
        return hash;
    }

    public void ensureVersioningEnabled(NodeRef nodeRef) {
        Map<QName, Serializable> versionProps = new HashMap<>();
        versionProps.put(ContentModel.PROP_INITIAL_VERSION, true);
        versionService.ensureVersioningEnabled(nodeRef, versionProps);
    }

    public String getCreateNewUrl(NodeRef nodeRef, String docExtMime){
        String folderNodeRef = this.nodeService.getPrimaryParent(nodeRef).getParentRef().toString();
        return getShareUrl() + "page/onlyoffice-edit?nodeRef=" + folderNodeRef + "&new=" + docExtMime;
    }

    public String getContentUrl(NodeRef nodeRef) {
        return  getAlfrescoUrl() + "s/parashift/onlyoffice/download?nodeRef=" + nodeRef.toString() + "&alf_ticket=" + authenticationService.getCurrentTicket();
    }

    public String getCallbackUrl(NodeRef nodeRef) {
        return getAlfrescoUrl() + "s/parashift/onlyoffice/callback?nodeRef=" + nodeRef.toString() + "&cb_key=" + getHash(nodeRef);
    }

    public String getTestConversionUrl() {
        return getAlfrescoUrl() + "s/parashift/onlyoffice/convertertest?alf_ticket=" + authenticationService.getCurrentTicket();
    }

    public String getEditorUrl() {
        return configManager.demoActive() ? configManager.getDemo("url") : (String) configManager.getOrDefault("url", "http://127.0.0.1/");
    }

    public String getBackUrl(NodeRef nodeRef){
        String path = "";
        while(this.nodeService.getPrimaryParent(nodeRef).getParentRef() != null){
            if(this.nodeService.getProperty(this.nodeService.getPrimaryParent(nodeRef).getParentRef(),
                    ContentModel.PROP_NAME).toString().equals(HOME_DIRECTORY)) {
                break;
            }
            path = ("/" + this.nodeService.getProperty(this.nodeService.getPrimaryParent(nodeRef).getParentRef(),
                    ContentModel.PROP_NAME)) + path;
            nodeRef=this.nodeService.getPrimaryParent(nodeRef).getParentRef();
        }
        path = "|" + path + "|";

        if(path.equals("||")){
            return getShareUrl() + "page/context/mine/myfiles";
        } else{
            try {
                return getShareUrl() + "page/context/mine/myfiles#filter=path" + java.net.URLEncoder.encode(path, String.valueOf(StandardCharsets.UTF_8));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public String getEditorInnerUrl() {
        String url = (String) configManager.getOrDefault("innerurl", "");
        if (url.isEmpty() || configManager.demoActive()) {
            return getEditorUrl();
        } else {
            return url;
        }
    }

    public String getEmbeddedSaveUrl(String sharedId, String docTitle) {
        StringBuilder embeddedSaveUrl = new StringBuilder(8);
        embeddedSaveUrl.append(UrlUtil.getShareUrl(sysAdminParams));
        embeddedSaveUrl.append("/proxy/alfresco-noauth/api/internal/shared/node/");
        embeddedSaveUrl.append(sharedId);
        embeddedSaveUrl.append("/content/");
        embeddedSaveUrl.append(URLEncoder.encodeUriComponent(docTitle));
        embeddedSaveUrl.append("?c=force");
        embeddedSaveUrl.append("&noCache=" + new Date().getTime());
        embeddedSaveUrl.append("&a=true");

        return embeddedSaveUrl.toString();
    }

    public String getEmbeddedSaveUrl(NodeRef nodeRef, String docTitle) {
        StringBuilder embeddedSaveUrl = new StringBuilder(7);
        StoreRef storeRef = nodeRef.getStoreRef();
        embeddedSaveUrl.append(UrlUtil.getShareUrl(sysAdminParams));
        embeddedSaveUrl.append("/proxy/alfresco/slingshot/node/content");
        embeddedSaveUrl.append("/" + storeRef.getProtocol());
        embeddedSaveUrl.append("/" + storeRef.getIdentifier());
        embeddedSaveUrl.append("/" + nodeRef.getId());
        embeddedSaveUrl.append("/" + URLEncoder.encodeUriComponent(docTitle));
        embeddedSaveUrl.append("?a=true");

        return embeddedSaveUrl.toString();
    }

    public String generateHash() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] token = new byte[32];
        secureRandom.nextBytes(token);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    }

    public NodeRef getNodeByPath(String path) {
        String storePath = "workspace://SpacesStore";
        StoreRef storeRef = new StoreRef(storePath);
        NodeRef storeRootNodeRef = nodeService.getRootNode(storeRef);
        List<NodeRef> nodeRefs = searchService.selectNodes(storeRootNodeRef, path, null, namespaceService, false);
        return nodeRefs.get(0);
    }

    public JSONArray getTemplates(NodeRef nodeRef, String docExt, String mimeType){
        JSONArray templates = new JSONArray();
        NodeRef templatesNodeRef = getNodeByPath("/app:company_home/app:dictionary/app:node_templates");
        List<ChildAssociationRef> assocs = nodeService.getChildAssocs(templatesNodeRef);
        for(ChildAssociationRef assoc : assocs){
            String docName = nodeService.getProperty(assoc.getChildRef(), ContentModel.PROP_NAME).toString();
            if(docExt.equals(docName.substring(docName.lastIndexOf(".") + 1))){
                JSONObject template = new JSONObject();
                String image = getShareUrl() + "proxy/alfresco/api/node/workspace/SpacesStore/" + assoc.getChildRef().toString().split("/SpacesStore/")[1] + "/content/thumbnails/doclib?ph=true";
                String title = nodeService.getProperty(assoc.getChildRef(), ContentModel.PROP_NAME).toString();
                String url = getCreateNewUrl(nodeRef, mimeType) + "&parentNodeRef=" + assoc.getChildRef();
                try {
                    template.put("image", image);
                    template.put("title", title);
                    template.put("url", url);
                    templates.put(template);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return templates;
    }

    private String getShareUrl(){
        return UrlUtil.getShareUrl(sysAdminParams) + "/";
    }

    private String getAlfrescoUrl() {
        String alfUrl = (String) configManager.getOrDefault("alfurl", "");
        if (alfUrl.isEmpty()) {
            return UrlUtil.getAlfrescoUrl(sysAdminParams) + "/";
        } else {
            return alfUrl + "alfresco/";
        }
    }

    public String getFileName(String url)
    {
        if (url == null) return "";

        String fileName = url.substring(url.lastIndexOf('/') + 1, url.length());
        fileName = fileName.split("\\?")[0];
        return fileName;
    }

    public String getCorrectName(NodeRef nodeFolder, String title, String ext) {
        String name = title + "." + ext;
        NodeRef node = nodeService.getChildByName(nodeFolder, ContentModel.ASSOC_CONTAINS, name);

        Integer i = 0;
        while (node != null) {
            i++;
            name = title + " (" + i + ")." + ext;
            node = nodeService.getChildByName(nodeFolder, ContentModel.ASSOC_CONTAINS, name);
        }
        return name;
    }

    public String getFileExtension(String url)
    {
        String fileName = getFileName(url);
        if (fileName == null) return null;
        String fileExt = fileName.substring(fileName.lastIndexOf("."));
        return fileExt.toLowerCase();
    }

    public String getDocType(String ext) {
        List<String> wordFormats = configManager.getListDefaultProperty("docservice.type.word");
        List<String> cellFormats = configManager.getListDefaultProperty("docservice.type.cell");
        List<String> slideFormats = configManager.getListDefaultProperty("docservice.type.slide");

        if (wordFormats.contains(ext)) return "text";
        if (cellFormats.contains(ext)) return "spreadsheet";
        if (slideFormats.contains(ext)) return "presentation";

        return null;
    }

    public boolean isEditable(String mime) {
        List<String> defaultEditableMimeTypes = configManager.getListDefaultProperty("docservice.mime.edit");
        Set<String> customizableEditableMimeTypes = configManager.getCustomizableEditableSet();
        return defaultEditableMimeTypes.contains(mime) || customizableEditableMimeTypes.contains(mime);
    }

    public String replaceDocEditorURLToInternal(String url) {
        String innerDocEditorUrl = getEditorInnerUrl();
        String publicDocEditorUrl = getEditorUrl();
        if (!publicDocEditorUrl.equals(innerDocEditorUrl)) {
            url = url.replace(publicDocEditorUrl, innerDocEditorUrl);
        }
        return url;
    }
}
