package app.medusa.nl.medusa;

import android.app.Application;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.content.Context;

public class CustomWebViewClient extends WebViewClient {

    static final String TAG = "Medusa";

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url)
	{
         view.loadUrl(url);
         return true;
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        //fetchContacts(view);
    }

    public void fetchContacts(WebView myWebView) {
        String phoneNumber = null;
        String email = null;

        Uri CONTENT_URI = ContactsContract.Contacts.CONTENT_URI;
        String _ID = ContactsContract.Contacts._ID;
        String DISPLAY_NAME = ContactsContract.Contacts.DISPLAY_NAME;
        String HAS_PHONE_NUMBER = ContactsContract.Contacts.HAS_PHONE_NUMBER;

        Uri PhoneCONTENT_URI = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String Phone_CONTACT_ID = ContactsContract.CommonDataKinds.Phone.CONTACT_ID;
        String NUMBER = ContactsContract.CommonDataKinds.Phone.NUMBER;

        Uri EmailCONTENT_URI =  ContactsContract.CommonDataKinds.Email.CONTENT_URI;
        String EmailCONTACT_ID = ContactsContract.CommonDataKinds.Email.CONTACT_ID;
        String DATA = ContactsContract.CommonDataKinds.Email.DATA;

        StringBuffer output = new StringBuffer();

        Context context = myWebView.getContext();

        ContentResolver contentResolver = context.getContentResolver();

        Cursor cursor = contentResolver.query(CONTENT_URI, null,null, null, null);
        String javascript;

        // Loop for every contact in the phone
        if (cursor.getCount() > 0) {

            while (cursor.moveToNext()) {

                String contact_id = cursor.getString(cursor.getColumnIndex( _ID ));
                String name = cursor.getString(cursor.getColumnIndex( DISPLAY_NAME ));

                int hasPhoneNumber = Integer.parseInt(cursor.getString(cursor.getColumnIndex( HAS_PHONE_NUMBER )));

                if (hasPhoneNumber > 0) {

                    // output.append("\n First Name:" + name);

                    //Log.i(TAG, name);
                    javascript = "javascript:addContact('"+name+"', '');";
                    myWebView.loadUrl(javascript);
                    // Query and loop for every phone number of the contact
                    Cursor phoneCursor = contentResolver.query(PhoneCONTENT_URI, null, Phone_CONTACT_ID + " = ?", new String[] { contact_id }, null);

                    while (phoneCursor.moveToNext()) {
                        phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(NUMBER));
                        //output.append("\n Phone number:" + phoneNumber);
                        //Log.i(TAG, phoneNumber);
                        javascript = "javascript:addPhone('"+phoneNumber+"');";
                        myWebView.loadUrl(javascript);
                    }

                    phoneCursor.close();
                    Cursor emailCursor = contentResolver.query(EmailCONTENT_URI,	null, EmailCONTACT_ID+ " = ?", new String[] { contact_id }, null);

                    while (emailCursor.moveToNext()) {

                        email = emailCursor.getString(emailCursor.getColumnIndex(DATA));

                        //output.append("\nEmail:" + email);
                        //Log.i(TAG, email);
                        javascript = "javascript:addEmail('"+email+"');";
                        myWebView.loadUrl(javascript);
                    }

                    emailCursor.close();
                }
                output.append("\n");
            }
        }
    }
}



