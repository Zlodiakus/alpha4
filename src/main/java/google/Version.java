package google;

import utility.StringUtils;

/**
 * Created by Shadilan on 28.05.2016.
 * Используется для проверки версий
 */
public class Version {
    public static boolean checkVersion(String version){
        return  "3.0.7a".equals(version)||
                "3.0.8a".equals(version)||
                "3.0.9a".equals(version)||
                "3.0.10a".equals(version)||
                "3.0.11a".equals(version)||
                "3.0.12a".equals(version)||
                "3.0.13a".equals(version)
                ;
    }
    public static boolean checkHash(String hash,String version,String operation,String... params){
        String code;
        switch (version){
            case "3.0.7a":code="COWBOW";
                break;
            case "3.0.8a":code="COWBOW";
                break;
            case "3.0.9a":code="COWBOW";
                break;
            case "3.0.10a":code="COWBOW";
                break;
            case "3.0.11a":code="COWBOW";
                break;
            case "3.0.12a":code="COWBOW";
                break;
            case "3.0.13a":code="COWBOW";
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
