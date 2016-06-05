package google;

import utility.StringUtils;

/**
 * Created by Shadilan on 28.05.2016.
 * Используется для проверки версий
 */
public class Version {
    public static boolean checkVersion(String version){
        return "2.0.18a".equals(version) ||
                "2.0.19a".equals(version)|| "3.0.0a".equals(version);
    }
    public static boolean checkHash(String hash,String version,String operation,String... params){
        String code;
        switch (version){
            case "2.0.18a":code="COWBOW";
                break;
            case "2.0.19a":code="COWBOW";
                break;
            case "3.0.0a":code="COWBOW";
                break;
            default: return false;
        }
        String param="";
        for (String s:params) {
            param+=s;
        }
        String correctHash= StringUtils.MD5(code + param + version + operation);
        return hash.equals(correctHash);
    }
}
