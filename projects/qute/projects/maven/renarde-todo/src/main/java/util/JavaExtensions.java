package util;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import io.quarkus.qute.TemplateExtension;

@TemplateExtension
public class JavaExtensions {

    public static boolean isRecent(Date date){
        Date now = new Date();
        Calendar cal = new GregorianCalendar();
        cal.add(Calendar.MONTH, -6);
        Date sixMonthsAgo = cal.getTime();
        return date.before(now) && date.after(sixMonthsAgo);
    }

}
