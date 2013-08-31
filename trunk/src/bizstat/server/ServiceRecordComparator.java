/*
 *    https://code.google.com/p/vellum - Contributed by Evan Summers
 */
package bizstat.server;

import bizstat.entity.ServiceRecord;
import java.util.Comparator;

/**
 *
 * @author evan.summers
 */
public class ServiceRecordComparator implements Comparator<ServiceRecord> {

    @Override
    public int compare(ServiceRecord o1, ServiceRecord o2) {
        return o1.getHostServiceKey().compareTo(o2.getHostServiceKey());
    }
}
