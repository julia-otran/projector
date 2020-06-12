/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package us.guihouse.projector.other;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 *
 * @author guilherme
 */
public class ProjectorStringUtils {
    private static final Pattern NON_ACSII = Pattern.compile("[^\\p{ASCII}]");
    
    public static String noAccents(String string) {
        String normalized = Normalizer.normalize(string.toLowerCase(), Normalizer.Form.NFD);
        return NON_ACSII.matcher(normalized).replaceAll("");
    }
}
