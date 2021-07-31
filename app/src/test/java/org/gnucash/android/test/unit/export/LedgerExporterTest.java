package org.gnucash.android.test.unit.export;

import android.database.sqlite.SQLiteDatabase;

import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.BookDbHelper;
import org.gnucash.android.db.DatabaseHelper;
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.db.adapter.BooksDbAdapter;
import org.gnucash.android.export.ExportFormat;
import org.gnucash.android.export.ExportParams;
import org.gnucash.android.export.ledger.LedgerExporter;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.Book;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.Split;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.test.unit.testutil.ShadowCrashlytics;
import org.gnucash.android.test.unit.testutil.ShadowUserVoice;
import org.gnucash.android.util.TimestampHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

//package is required so that resources can be found in dev mode
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 21,
        packageName = "org.gnucash.android",
        shadows = {ShadowCrashlytics.class, ShadowUserVoice.class})
public class LedgerExporterTest {
    private SQLiteDatabase mDb;

    @Before
    public void setUp() throws Exception {
        BookDbHelper bookDbHelper = new BookDbHelper(GnuCashApplication.getAppContext());
        BooksDbAdapter booksDbAdapter = new BooksDbAdapter(bookDbHelper.getWritableDatabase());
        Book testBook = new Book("testRootAccountUID");
        booksDbAdapter.addRecord(testBook);
        DatabaseHelper databaseHelper =
                new DatabaseHelper(GnuCashApplication.getAppContext(), testBook.getUID());
        mDb = databaseHelper.getWritableDatabase();
    }

    /**
     * When there aren't new or modified transactions, the ledger exporter
     * shouldn't create any file.
     */
    @Test
    public void testWithNoTransactionsToExport_shouldNotCreateAnyFile() {
        ExportParams exportParameters = new ExportParams(ExportFormat.LEDGER);
        exportParameters.setExportStartTime(TimestampHelper.getTimestampFromEpochZero());
        exportParameters.setExportTarget(ExportParams.ExportTarget.SD_CARD);
        exportParameters.setDeleteTransactionsAfterExport(false);
        LedgerExporter exporter = new LedgerExporter(exportParameters, mDb);
        assertThat(exporter.generateExport()).isEmpty();
    }

    /**
     * Test that ledger files are generated
     */
    @Test
    public void testGenerateLedgerExport() {
        AccountsDbAdapter accountsDbAdapter = new AccountsDbAdapter(mDb);

        Account account = new Account("Basic Account");
        Transaction transaction = new Transaction("One transaction");
        transaction.addSplit(new Split(Money.createZeroInstance("EUR"), account.getUID()));
        account.addTransaction(transaction);

        accountsDbAdapter.addRecord(account);

        ExportParams exportParameters = new ExportParams(ExportFormat.LEDGER);
        exportParameters.setExportStartTime(TimestampHelper.getTimestampFromEpochZero());
        exportParameters.setExportTarget(ExportParams.ExportTarget.SD_CARD);
        exportParameters.setDeleteTransactionsAfterExport(false);

        LedgerExporter exporter = new LedgerExporter(exportParameters, mDb);
        List<String> exportedFiles = exporter.generateExport();

        assertThat(exportedFiles).hasSize(1);
        File file = new File(exportedFiles.get(0));
        assertThat(file).exists().hasExtension("ledger");
        assertThat(file.length()).isGreaterThan(0L);
    }
}
