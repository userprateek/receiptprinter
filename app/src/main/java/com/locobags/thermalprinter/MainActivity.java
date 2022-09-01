package com.locobags.thermalprinter;

import static android.widget.Toast.LENGTH_SHORT;
import static android.widget.Toast.makeText;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputLayout;
import com.locobags.escposprinter.connection.DeviceConnection;
import com.locobags.escposprinter.connection.bluetooth.BluetoothConnection;
import com.locobags.escposprinter.connection.bluetooth.BluetoothPrintersConnections;
import com.locobags.thermalprinter.async.AsyncBluetoothEscPosPrint;
import com.locobags.thermalprinter.async.AsyncEscPosPrint;
import com.locobags.thermalprinter.async.AsyncEscPosPrinter;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private TextInputLayout partyName, til_item_name, til_quantity, til_rate;
    private LinearLayout inputRow1, inputRow2, inputRow3, inputRow4, inputRow5, addRow;
    private Button bluetoothBrowse, bluetoothPrint;
    private AutoCompleteTextView actvItemName;
    private TextView tvGrandTotal;
    private String printString = "";
    private int storedRate;
    private int visibleRow = 1;
    int[] total = new int[5];
    private int grandTotal = 0;
    private static final String[] ItemTypes = new String[]{
            "Duffle Bag", "Gym Bag", "Backpack Bag", "Other", "Other"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sharedPreferences = getSharedPreferences("locobagsPref", MODE_PRIVATE);
        SharedPreferences.Editor myEdit = sharedPreferences.edit();
        setContentView(R.layout.activity_main);
        partyName = findViewById(R.id.til_party_name);

        inputRow1 = findViewById(R.id.ll_input_row1);
        inputRow2 = findViewById(R.id.ll_input_row2);
        inputRow3 = findViewById(R.id.ll_input_row3);
        inputRow4 = findViewById(R.id.ll_input_row4);
        inputRow5 = findViewById(R.id.ll_input_row5);
        addRow = findViewById(R.id.ll_add_row);
        tvGrandTotal = findViewById(R.id.tv_grand_total);
        til_item_name = inputRow1.findViewById(R.id.til_item_name);
        actvItemName = inputRow1.findViewById(R.id.actv_item_name);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line, ItemTypes);
        int[] ids = new int[]{R.id.ll_input_row1, R.id.ll_input_row2, R.id.ll_input_row3, R.id.ll_input_row4, R.id.ll_input_row5};

        actvItemName.setAdapter(adapter);
        actvItemName.setText(ItemTypes[visibleRow - 1]);
        til_rate = inputRow1.findViewById(R.id.til_rate);
        til_quantity = inputRow1.findViewById(R.id.til_quantity);
        bluetoothBrowse = this.findViewById(R.id.button_bluetooth_browse);
        bluetoothPrint = findViewById(R.id.button_bluetooth);
        bluetoothBrowse.setOnClickListener(view -> browseBluetoothDevice());
        bluetoothPrint.setOnClickListener(view -> {
            if (validateInput()) {
                if (til_rate.getEditText().getText().toString() != "") {
                    if (storedRate != Integer.parseInt(til_rate.getEditText().getText().toString())) {
                        myEdit.putInt("rate", Integer.parseInt(til_rate.getEditText().getText().toString()));
                        myEdit.apply();
                    }
                }
                printBluetooth();
            }
        });
        addRow.setOnClickListener(view -> {
            visibleRow++;
            actvItemName = findViewById(ids[visibleRow - 1]).findViewById(R.id.actv_item_name);
            actvItemName.setAdapter(adapter);
            actvItemName.setText(ItemTypes[visibleRow - 1]);
            switch (visibleRow) {
                case 2: {
                    inputRow2.setVisibility(View.VISIBLE);
                    break;
                }
                case 3: {
                    inputRow3.setVisibility(View.VISIBLE);
                    break;
                }
                case 4: {
                    inputRow4.setVisibility(View.VISIBLE);
                    break;
                }
                case 5: {
                    inputRow5.setVisibility(View.VISIBLE);
                    addRow.setVisibility(View.GONE);
                    break;
                }
                default: {
                    addRow.setVisibility(View.GONE);
                    break;
                }
            }
        });
        int rate = sharedPreferences.getInt("rate", 0);
        if (rate > 0) {
            storedRate = rate;
            til_rate.getEditText().setText(String.valueOf(rate));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

    }


    /*==============================================================================================
    ======================================BLUETOOTH PART============================================
    ==============================================================================================*/

    public static final int PERMISSION_BLUETOOTH = 1;
    public static final int PERMISSION_BLUETOOTH_ADMIN = 2;
    public static final int PERMISSION_BLUETOOTH_CONNECT = 3;
    public static final int PERMISSION_BLUETOOTH_SCAN = 4;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            switch (requestCode) {
                case MainActivity.PERMISSION_BLUETOOTH:
                case MainActivity.PERMISSION_BLUETOOTH_ADMIN:
                case MainActivity.PERMISSION_BLUETOOTH_CONNECT:
                case MainActivity.PERMISSION_BLUETOOTH_SCAN:
                    this.printBluetooth();
                    break;
            }
        }
    }

    private BluetoothConnection selectedDevice;

    public void browseBluetoothDevice() {
        final BluetoothConnection[] bluetoothDevicesList = (new BluetoothPrintersConnections()).getList();

        if (bluetoothDevicesList != null) {
            final String[] items = new String[bluetoothDevicesList.length + 1];
            items[0] = "Default printer";
            int i = 0;
            for (BluetoothConnection device : bluetoothDevicesList) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                items[++i] = device.getDevice().getName();
            }

            AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
            alertDialog.setTitle("Bluetooth printer selection");
            alertDialog.setItems(items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    int index = i - 1;
                    if (index == -1) {
                        selectedDevice = null;
                    } else {
                        selectedDevice = bluetoothDevicesList[index];
                    }
                    Button button = (Button) findViewById(R.id.button_bluetooth_browse);
                    button.setText(items[i]);
                }
            });

            AlertDialog alert = alertDialog.create();
            alert.setCanceledOnTouchOutside(false);
            alert.show();

        }
    }

    public boolean validateInput() {
        int[] ids = new int[]{R.id.ll_input_row1, R.id.ll_input_row2, R.id.ll_input_row3, R.id.ll_input_row4, R.id.ll_input_row5};
        TextInputLayout til_quantity, til_rate;
        for (int i = 0; i < visibleRow; i++) {
            til_quantity = findViewById(ids[i]).findViewById(R.id.til_quantity);
            til_rate = findViewById(ids[i]).findViewById(R.id.til_rate);
            if (partyName.getEditText().getText().toString().equals("")) {
                makeText(this, String.format("%s %s", getString(R.string.enter), getString(R.string.party_name)), LENGTH_SHORT).show();
                return false;
            } else if (til_quantity.getEditText().getText().toString().equals("")) {
                makeText(this, String.format("%s %s", getString(R.string.enter), getString(R.string.quantity)), LENGTH_SHORT).show();
                return false;
            } else if (til_rate.getEditText().getText().toString().equals("")) {
                makeText(this, String.format("%s %s", getString(R.string.enter), getString(R.string.rate)), LENGTH_SHORT).show();
                return false;
            }
        }

        return true;
    }

    private void calculateGrandTotal() {
        int[] ids = new int[]{R.id.ll_input_row1, R.id.ll_input_row2, R.id.ll_input_row3, R.id.ll_input_row4, R.id.ll_input_row5};
        TextInputLayout til_item_name, til_quantity, til_rate;
        for (int i = 0; i < visibleRow; i++) {
            til_quantity = findViewById(ids[i]).findViewById(R.id.til_quantity);
            til_rate = findViewById(ids[i]).findViewById(R.id.til_rate);
            til_item_name = findViewById(ids[i]).findViewById(R.id.til_item_name);

            total[i] += Integer.parseInt(til_quantity.getEditText().getText().toString()) * Integer.parseInt(til_rate.getEditText().getText().toString());
            grandTotal += total[i];
            printString += "[L]<b>" + til_item_name.getEditText().getText() + "</b>[R] " + Integer.parseInt(til_quantity.getEditText().getText().toString()) + " [R] " + Integer.parseInt(til_rate.getEditText().getText().toString()) + "[R] " + total[i] + " \n";
        }
    }
    private void clearFields() {
        int[] ids = new int[]{R.id.ll_input_row1, R.id.ll_input_row2, R.id.ll_input_row3, R.id.ll_input_row4, R.id.ll_input_row5};
        TextInputLayout til_quantity;
        for (int i = 0; i < 5; i++) {
            til_quantity = findViewById(ids[i]).findViewById(R.id.til_quantity);
            til_quantity.getEditText().setText("");
            if (i != 0) {
                findViewById(ids[i]).setVisibility(View.GONE);
            }
            total[i]=0;
        }
        visibleRow = 1;
        grandTotal = 0;
        printString = "";
    }
    public void printBluetooth() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH}, MainActivity.PERMISSION_BLUETOOTH);
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_ADMIN}, MainActivity.PERMISSION_BLUETOOTH_ADMIN);
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, MainActivity.PERMISSION_BLUETOOTH_CONNECT);
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, MainActivity.PERMISSION_BLUETOOTH_SCAN);
        } else {
            new AsyncBluetoothEscPosPrint(
                    this,
                    new AsyncEscPosPrint.OnPrintFinished() {
                        @Override
                        public void onError(AsyncEscPosPrinter asyncEscPosPrinter, int codeException) {
                            Log.e("Async.OnPrintFinished", "AsyncEscPosPrint.OnPrintFinished : An error occurred !");
                        }

                        @Override
                        public void onSuccess(AsyncEscPosPrinter asyncEscPosPrinter) {
                            clearFields();
                            Log.i("Async.OnPrintFinished", "AsyncEscPosPrint.OnPrintFinished : Print is finished !");
                        }
                    }
            )
                    .execute(this.getAsyncEscPosPrinter(selectedDevice));
        }
    }
    /*==============================================================================================
    ===================================ESC/POS PRINTER PART=========================================
    ==============================================================================================*/

    /**
     * Asynchronous printing
     */
    @SuppressLint("SimpleDateFormat")
    public AsyncEscPosPrinter getAsyncEscPosPrinter(DeviceConnection printerConnection) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss");
        calculateGrandTotal();
        AsyncEscPosPrinter printer = new AsyncEscPosPrinter(printerConnection, 203, 48f, 32);

        return printer.addTextToPrint(
                "[C]<u><font size='big'>LOCOBAGS</font></u>\n" +
                        "[L]\n" +
//                "[C]<img>" + PrinterTextParserImg.bitmapToHexadecimalString(printer, this.getApplicationContext().getResources().getDrawableForDensity(R.drawable.logo_black_white, DisplayMetrics.DENSITY_MEDIUM)) + "</img>\n" +
//                        "[L]\n"+
                        "[C]<u type='double'>" + format.format(new Date()) + "</u>\n" +
                        "[C]================================\n" +
                        "[C]<u type='double'>" + partyName.getEditText().getText().toString() + "</u>\n" +
                        "[L]\n" +
                        "[L]<b>" + til_item_name.getHint().toString() + "</b>[R]  Qty [R] Rate [R] Total \n" +
                        printString +
                        "[C]--------------------------------\n" +
                        "[C]Grand Total : ₹" + grandTotal + "\n"
        );
    }
}
/*
"[C]<img>" + PrinterTextParserImg.bitmapToHexadecimalString(printer, this.getApplicationContext().getResources().getDrawableForDensity(R.drawable.logo, DisplayMetrics.DENSITY_MEDIUM)) + "</img>\n" +
                        "[L]\n"
"[C]<u><font size='big'>ORDER N°045</font></u>\n" +
                        "[L]\n" +
                        "[C]<u type='double'>" + format.format(new Date()) + "</u>\n" +
                        "[C]\n" +
                        "[C]================================\n" +
                        "[L]\n" +
                        "[L]<b>BEAUTIFUL SHIRT</b>[R]9.99€\n" +
                        "[L]  + Size : S\n" +
                        "[L]\n" +
                        "[L]<b>AWESOME HAT</b>[R]24.99€\n" +
                        "[L]  + Size : 57/58\n" +
                        "[L]\n" +
                        "[C]--------------------------------\n" +
                        "[R]TOTAL PRICE :[R]34.98€\n" +
                        "[R]TAX :[R]4.23€\n" +
                        "[L]\n" +
                        "[C]================================\n" +
                        "[L]\n" +
                        "[L]<u><font color='bg-black' size='tall'>Customer :</font></u>\n" +
                        "[L]Raymond DUPONT\n" +
                        "[L]5 rue des girafes\n" +
                        "[L]31547 PERPETES\n" +
                        "[L]Tel : +33801201456\n" +
                        "\n" +
                        "[C]<barcode type='ean13' height='10'>831254784551</barcode>\n" +
                        "[L]\n" +
                        "[C]<qrcode size='20'>http://www.developpeur-web.dantsu.com/</qrcode>\n"

 */