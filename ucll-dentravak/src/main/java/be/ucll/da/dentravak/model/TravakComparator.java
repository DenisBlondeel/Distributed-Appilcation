package be.ucll.da.dentravak.model;

import java.util.Comparator;
import java.util.Map;
import java.util.UUID;


    public class TravakComparator implements Comparator<Map.Entry<UUID, Float>> {
        public int compare(Map.Entry<UUID, Float> o1, Map.Entry<UUID, Float> o2) {
            return o1.getValue().compareTo(o2.getValue());
        }
    }
