package ch.hevs.cloudio.endpoint;

import java.util.HashMap;

public class CloudioMessageFormatFactory {
    public static CloudioMessageFormat massageFormat(int messageFormatId) {
        if (formats.containsKey(messageFormatId)) {
            return formats.get(messageFormatId);
        } else {
            CloudioMessageFormat format = null;
            switch (messageFormatId) {
                case '{':
                    format = new JsonMessageFormat();
                    formats.put((int)'{', format);
                    break;

                case 'z':
                    format = new JsonZipMessageFormat();
                    formats.put((int)'z', format);
                    break;

                default:
                    break;
            }

            return format;
        }
    }

    private static final HashMap<Integer,CloudioMessageFormat> formats = new HashMap<Integer,CloudioMessageFormat>();
}
