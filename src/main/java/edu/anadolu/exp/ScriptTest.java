package edu.anadolu.exp;

import com.ibm.icu.lang.UScript;
import com.ibm.icu.lang.UScriptRun;

import java.util.Arrays;

public class ScriptTest {

    public static void main(String[] args) {

        String text = "şeker 10 2 5 9 9 36 ahmet üzüm şeker";

        printScriptRuns(text.toCharArray());

        /** linear fast-path for basic latin case */
        final int basicLatin[] = new int[128];

        System.out.println(basicLatin.length);
        for (int i = 0; i < basicLatin.length; i++)
            basicLatin[i] = UScript.getScript(i);

        System.out.println(Arrays.toString(basicLatin));

        for (int i = 0; i < basicLatin.length; i++)
            System.out.println(UScript.getName(basicLatin[i]));

    }

    static void printScriptRuns(char[] text) {
        UScriptRun scriptRun = new UScriptRun(text);

        while (scriptRun.next()) {
            int start = scriptRun.getScriptStart();
            int limit = scriptRun.getScriptLimit();
            int script = scriptRun.getScriptCode();

            System.out.println("Script \"" + UScript.getName(script) + "\" from " +
                    start + " to " + limit + ".");
        }
    }
}
