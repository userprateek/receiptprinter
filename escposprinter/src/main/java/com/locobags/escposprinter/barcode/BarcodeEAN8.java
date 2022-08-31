package com.locobags.escposprinter.barcode;

import com.locobags.escposprinter.EscPosPrinterCommands;
import com.locobags.escposprinter.EscPosPrinterSize;
import com.locobags.escposprinter.exceptions.EscPosBarcodeException;

public class BarcodeEAN8 extends BarcodeNumber {
    public BarcodeEAN8(EscPosPrinterSize printerSize, String code, float widthMM, float heightMM, int textPosition) throws EscPosBarcodeException {
        super(printerSize, EscPosPrinterCommands.BARCODE_TYPE_EAN8, code, widthMM, heightMM, textPosition);
    }

    @Override
    public int getCodeLength() {
        return 8;
    }
}
