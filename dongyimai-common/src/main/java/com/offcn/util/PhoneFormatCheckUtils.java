package com.offcn.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PhoneFormatCheckUtils {

    public static boolean isPhoneLegal(String phone){
        Pattern p = null;
        Matcher m = null;
        boolean b = false;
        p = Pattern.compile("^[1][3,4,5,7,8][0-9]{9}$"); // 验证手机号
        m = p.matcher(phone);
        b = m.matches();
        return b;
   }
}
