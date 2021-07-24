package org.gnucash.android.export.ledger;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.gnucash.android.export.ExportParams;
import org.gnucash.android.export.Exporter;
import org.gnucash.android.model.Split;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.model.TransactionType;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LedgerExporter extends Exporter {

    private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    public LedgerExporter(ExportParams params) {
        super(params, null);
        LOG_TAG = "GncXmlExporter";
    }

    public LedgerExporter(ExportParams params, SQLiteDatabase db) {
        super(params, db);
        LOG_TAG = "GncXmlExporter";
    }

    @Override
    public List<String> generateExport() throws ExporterException {
        String outputFile = getExportCacheFilePath();
        try {
            Cursor cursor = mTransactionsDbAdapter.fetchTransactionsModifiedSince(mExportParams.getExportStartTime());
            File file = new File(getExportCacheFilePath());
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));
            while (cursor.moveToNext()) {
                Transaction transaction = mTransactionsDbAdapter.buildModelInstance(cursor);
                Date date = new Date(transaction.getTimeMillis());
                writer
                        .append(dateFormat.format(date))
                        .append(' ')
                        .append(transaction.getDescription())
                        .append('\n');
                for (Split split : transaction.getSplits()) {
                    String accountFullName = mAccountsDbAdapter.getAccountFullName(split.getAccountUID());
                    String sign = split.getType() == TransactionType.CREDIT ? "-" : "";
                    writer
                            .append("    ")
                            .append(accountFullName)
                            .append("    ")
                            .append(sign)
                            .append(split.getQuantity().toLocaleString())
                            .append('\n');
                }
                writer.append('\n');
            }
            writer.close();
        } catch (IOException e) {
            throw new ExporterException(mExportParams, e);
        }
        return Collections.singletonList(outputFile);
    }
}
