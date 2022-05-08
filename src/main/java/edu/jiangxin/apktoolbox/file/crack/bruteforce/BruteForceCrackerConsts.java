package edu.jiangxin.apktoolbox.file.crack.bruteforce;

import edu.jiangxin.apktoolbox.file.crack.cracker.ICracker;

public class BruteForceCrackerConsts {
    final int numThreads;
    final int passwordLength;
    final long passwordRangeSize;
    final long passwordSubRangeSize;
    final ICracker cracker;
    final String charsSet;

    public BruteForceCrackerConsts(int numThreads, int passwordLength, ICracker cracker, String charsSet) {
        this.numThreads = numThreads;
        this.passwordLength = passwordLength;
        this.cracker = cracker;
        this.passwordRangeSize = (long) Math.pow(charsSet.length(), passwordLength);
        this.passwordSubRangeSize = (passwordRangeSize + numThreads - 1) / numThreads;
        this.charsSet = charsSet;
    }

    public int getPasswordLength() {
        return passwordLength;
    }

    public long getPasswordTotalRangeSize() {
        return passwordRangeSize;
    }

    public long getPasswordSubRangeSize() {
        return passwordSubRangeSize;
    }

    public ICracker getCracker() {
        return cracker;
    }

    public String getCharsSet() {
        return charsSet;
    }
}

