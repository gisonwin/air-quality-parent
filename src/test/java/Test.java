import org.apache.bookkeeper.client.BKException;
import org.apache.bookkeeper.client.BookKeeper;
import org.apache.bookkeeper.client.LedgerEntry;
import org.apache.bookkeeper.client.LedgerHandle;
import org.apache.bookkeeper.client.api.ReadHandle;
import org.apache.bookkeeper.client.api.WriteHandle;
import org.apache.bookkeeper.conf.ClientConfiguration;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.Iterator;

/**
 * @author <a href="mailto:gisonwin@qq.com">Gison.Win</a>
 * @Description
 * @Date 2022/7/3 8:51
 */
public class Test {

    public static final String GISONWIN = "gisonwin";
    public static final String ZK_SERVER = "zk+hierarchical://192.168.236.188/ledgers";
    public static final int CONNECT_TIMEOUT_MILLIS = 3000000;

    static void test2() {
        try {
            ClientConfiguration config = new ClientConfiguration();
            config.setMetadataServiceUri(ZK_SERVER);
            config.setTimeoutMonitorIntervalSec(CONNECT_TIMEOUT_MILLIS);
            org.apache.bookkeeper.client.api.BookKeeper bkClient = org.apache.bookkeeper.client.api.BookKeeper.newBuilder(config).build();
            System.out.println("BookKeeper client init success.");

            WriteHandle writeHandle = bkClient.newCreateLedgerOp().withDigestType(org.apache.bookkeeper.client.api.DigestType.MAC).withPassword(GISONWIN.getBytes()).execute().get();


            for (int i = 0; i < 10; i++) {
                byte[] data = new String(GISONWIN + ":message-" + i).getBytes();
                writeHandle.append(data);
                System.out.println("write to ==> " + new String(data));
            }
            writeHandle.close();
            long ledgerId = writeHandle.getLedgerMetadata().getLedgerId();
            ReadHandle readHandle = bkClient.newOpenLedgerOp().withLedgerId(ledgerId).withDigestType(org.apache.bookkeeper.client.api.DigestType.MAC).withPassword(GISONWIN.getBytes()).execute().get();

            System.out.println("ledgerId = " + ledgerId);

            Iterator<org.apache.bookkeeper.client.api.LedgerEntry> iterable = readHandle.read(0, writeHandle.getLastAddConfirmed()).iterator();
            while (iterable.hasNext()) {
                org.apache.bookkeeper.client.api.LedgerEntry entry = iterable.next();
                System.out.println("read => " + new String(entry.getEntryBytes()));
            }
            readHandle.close();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    static void test() throws BKException, IOException, InterruptedException {
        ClientConfiguration config = new ClientConfiguration();
        config.setMetadataServiceUri(ZK_SERVER);
        //这里注意一定要将超时时间 设置长一点。不然会一直报错。
        config.setClientConnectTimeoutMillis(CONNECT_TIMEOUT_MILLIS);
        BookKeeper bkc = new BookKeeper(config);
        // A password for the new ledger
        byte[] ledgerPassword = GISONWIN.getBytes();

        // Create a new ledger and fetch its identifier
        LedgerHandle lh = bkc.createLedger(BookKeeper.DigestType.MAC, ledgerPassword);
        long ledgerId = lh.getId();

        // Create a buffer for four-byte entries
        ByteBuffer entry = ByteBuffer.allocate(4);

        int numberOfEntries = 10;

        // Add entries to the ledger, then close it
        for (int i = 0; i < numberOfEntries; i++) {
            entry.putInt(i);
            entry.position(0);
            lh.addEntry(entry.array());
        }
        lh.close();

        // Open the ledger for reading
        lh = bkc.openLedger(ledgerId, BookKeeper.DigestType.MAC, ledgerPassword);

        // Read all available entries
        Enumeration<LedgerEntry> entries = lh.readEntries(0, numberOfEntries - 1);

        while (entries.hasMoreElements()) {
            ByteBuffer result = ByteBuffer.wrap(entries.nextElement().getEntry());
            Integer retrEntry = result.getInt();

            // Print the integer stored in each entry
            System.out.println(String.format("Result: %s", retrEntry));
        }

        // Close the ledger and the client
        lh.close();
        bkc.close();

    }

    public static void main(String[] args) {
        test2();
    }
}
