package com.locobags.escposprinter.textparser;

import com.locobags.escposprinter.EscPosPrinterCommands;
import com.locobags.escposprinter.exceptions.EscPosConnectionException;
import com.locobags.escposprinter.exceptions.EscPosEncodingException;

public interface IPrinterTextParserElement {
    int length() throws EscPosEncodingException;
    IPrinterTextParserElement print(EscPosPrinterCommands printerSocket) throws EscPosEncodingException, EscPosConnectionException;
}
