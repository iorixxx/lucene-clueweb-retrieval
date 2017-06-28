package edu.anadolu.datasets;

import edu.anadolu.mc.MCSet;

/**
 * Factory responsible for creating {@link DataSet} from {@link Collection} command line argument
 */
public class CollectionFactory {

    public static DataSet dataset(Collection collection, String tfd_home) {

        if (collection == null) throw new RuntimeException("collection is null!");

        switch (collection) {
            case CW09A:
                return new ClueWeb09A(tfd_home);

            case CW09B:
                return new ClueWeb09B(tfd_home);

            case CW12B:
                return new ClueWeb12B(tfd_home);

            case MC:
                return new MCSet(tfd_home);

            case MQ08:
                return new MQ08(tfd_home);

            case MQ09:
                return new MQ09(tfd_home);

            case GOV2:
                return new Gov2(tfd_home);

            case ROB04:
                return new ROB04(tfd_home);

            case MQE1:
                return new MQE1(tfd_home);

            default:
                throw new RuntimeException(collection + " returned null DataSet");

        }
    }
}
