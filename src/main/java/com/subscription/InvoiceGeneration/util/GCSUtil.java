package com.subscription.InvoiceGeneration.util;

import com.google.auth.oauth2.GoogleCredentials;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class GCSUtil {
    private static final Logger LOG = LogManager.getLogger();

    public static GoogleCredentials getCredentials (String service_acct_key) {
        GoogleCredentials credentials = null;
        try {
            credentials = GoogleCredentials.fromStream(
                    new FileInputStream (service_acct_key)
            );
        } catch (FileNotFoundException fe) {
            LOG.error ("File does not exists in the specified path : {}", fe.getMessage());
        } catch (IOException  ioe) {
            LOG.error ("Error with reading the file : {}", ioe.getMessage());
        }
        return credentials;
    }

}
