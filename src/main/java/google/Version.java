package google;

import utility.StringUtils;

/**
 * Created by Shadilan on 28.05.2016.
 * Используется для проверки версий
 */
public class Version {
    public static boolean checkVersion(String version){
        return  "3.0.1a".equals(version)||
                "3.0.2a".equals(version)||
                "3.0.3a".equals(version)||
                "3.0.4a".equals(version)||
                "3.0.5a".equals(version)||
                "3.0.6a".equals(version)
                ;
    }
    public static boolean checkHash(String hash,String version,String operation,String... params){
        String code;
        switch (version){
            case "3.0.1a":code="COWBOW";
                break;
            case "3.0.2a":code="COWBOW";
                break;
            case "3.0.3a":code="COWBOW";
                break;
            case "3.0.4a":code="COWBOW";
                break;
            case "3.0.5a":code="COWBOW";
                break;
            case "3.0.6a":code="COWBOW";
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
