/**
 * 
 */
package com.nanosheep.bikeroute;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

/**
 * Activity for sending feedback on a route to CycleStreets.net.
 * 
 * @author jono@nanosheep.net
 * @version Nov 11, 2010
 */
public class Feedback extends Activity {
	private BikeRouteApp app;
	private TextView nameField;
	private TextView emailField;
	private TextView commentField;
	private Button submit;

	@Override
	public final void onCreate(final Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	app = ((BikeRouteApp) getApplication());
	requestWindowFeature(Window.FEATURE_RIGHT_ICON);
	setContentView(R.layout.feedback);
	setFeatureDrawableResource(Window.FEATURE_RIGHT_ICON, R.drawable.ic_bar_bikeroute);
	
	nameField = (TextView) findViewById(R.id.name_input);
	emailField = (TextView) findViewById(R.id.email_input);
	commentField = (TextView) findViewById(R.id.comment_input);
	submit = (Button) findViewById(R.id.submit_button);
	
	//Handle rotations
	final Object[] data = (Object[]) getLastNonConfigurationInstance();
	if (data != null) {
		nameField.setText((CharSequence) data[0]);
		emailField.setText((CharSequence) data[1]);
		commentField.setText((CharSequence) data[2]);
	}
	//Input validation
	final Validate watcher = new Validate();
	nameField.addTextChangedListener(watcher);
	emailField.addTextChangedListener(watcher);
	commentField.addTextChangedListener(watcher);
	
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		Object[] objs = new Object[3];
		objs[0] = nameField.getText();
		objs[1] = emailField.getText();
		objs[2] = commentField.getText();
	    return objs;
	}
	
	private class SubmitHandler extends AsyncTask<Object, Object, Object> implements OnClickListener {
		/* (non-Javadoc)
		 * @see android.view.View.OnClickListener#onClick(android.view.View)
		 */
		@Override
		public void onClick(View arg0) {
			this.execute();
		}

		/**
		 * Fire a thread to submit feedback.
		 * 
		 * @param itineraryId
		 * @param name
		 * @param email
		 * @param comment
		 */
		private void doSubmit(final int itineraryId, final String name, final String email,
				final String comment) {
			String reqString = getString(R.string.feedback_api);
			reqString += "itinerary=" + itineraryId;
			reqString += "&comments=" + URLEncoder.encode(comment);
			reqString += "&name=" + URLEncoder.encode(name);
			reqString += "&email=" + URLEncoder.encode(email);
			HttpUriRequest request = new HttpPut(reqString);
	        try {
				final HttpResponse response = new DefaultHttpClient().execute(request);
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		/* (non-Javadoc)
		 * @see android.os.AsyncTask#doInBackground(Params[])
		 */
		@Override
		protected Object doInBackground(Object... arg0) {
			doSubmit(app.getRoute().getItineraryId(), nameField.getText().toString(), 
					emailField.getText().toString(), commentField.getText().toString());
			return null;
		}
		
	}
	
	private class Validate implements TextWatcher {
		Pattern p;
		
		public Validate() {
			p = Pattern.compile("((([ \\t]*[a-zA-Z0-9\\!\\#\\$\\%\\&\\'\\*\\+\\-\\/\\=\\?\\^\\_\\`\\{\\|\\}\\~]+[ \\t]*)|(\\\"([ \\t]*([\\x01-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F\\x21\\x23-\\x5B\\x5D-\\x7E]|(\\\\[\\x01-\\x09\\x0B\\x0C\\x0E-\\x7F])))*[ \\t]*\\\"))+)?[ \\t]*<(([ \\t]*([a-zA-Z0-9\\!\\#\\$\\%\\&\\'\\*\\+\\-\\/\\=\\?\\^\\_\\`\\{\\|\\}\\~]+(\\.[a-zA-Z0-9\\!\\#\\$\\%\\&\\'\\*\\+\\-\\/\\=\\?\\^\\_\\`\\{\\|\\}\\~]+)*)[ \\t]*)|(\\\"([ \\t]*([\\x01-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F\\x21\\x23-\\x5B\\x5D-\\x7E]|(\\\\[\\x01-\\x09\\x0B\\x0C\\x0E-\\x7F])))*[ \\t]*\\\"))@([ \\t]*([a-zA-Z0-9\\!\\#\\$\\%\\&\\'\\*\\+\\-\\/\\=\\?\\^\\_\\`\\{\\|\\}\\~]+(\\.[a-zA-Z0-9\\!\\#\\$\\%\\&\\'\\*\\+\\-\\/\\=\\?\\^\\_\\`\\{\\|\\}\\~]+)*)[ \\t]*|\\[([ \\t]*[\\x01-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F\\x21-\\x5A\\x5E-\\x7E]|(\\\\[\\x01-\\x09\\x0B\\x0C\\x0E-\\x7F])+)*[ \\t]*\\])>|(([ \\t]*([a-zA-Z0-9\\!\\#\\$\\%\\&\\'\\*\\+\\-\\/\\=\\?\\^\\_\\`\\{\\|\\}\\~]+(\\.[a-zA-Z0-9\\!\\#\\$\\%\\&\\'\\*\\+\\-\\/\\=\\?\\^\\_\\`\\{\\|\\}\\~]+)*)[ \\t]*)|(\\\"([ \\t]*([\\x01-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F\\x21\\x23-\\x5B\\x5D-\\x7E]|(\\\\[\\x01-\\x09\\x0B\\x0C\\x0E-\\x7F])))*[ \\t]*\\\"))@([ \\t]*([a-zA-Z0-9\\!\\#\\$\\%\\&\\'\\*\\+\\-\\/\\=\\?\\^\\_\\`\\{\\|\\}\\~]+(\\.[a-zA-Z0-9\\!\\#\\$\\%\\&\\'\\*\\+\\-\\/\\=\\?\\^\\_\\`\\{\\|\\}\\~]+)*)[ \\t]*|\\[([ \\t]*[\\x01-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F\\x21-\\x5A\\x5E-\\x7E]|(\\\\[\\x01-\\x09\\x0B\\x0C\\x0E-\\x7F])+)*[ \\t]*\\])");
		}
		
		/* (non-Javadoc)
		 * @see android.text.TextWatcher#afterTextChanged(android.text.Editable)
		 */
		@Override
		public void afterTextChanged(Editable arg0) {
			if ((commentField.getText().length() != 0) && isValid(emailField.getText())) {
				submit.setEnabled(true);
			} else {
				submit.setEnabled(false);
			}
		}

		/**
		 * @param text
		 * @return
		 */
		private boolean isValid(CharSequence text) {
			return p.matcher(text).matches();
		}

		/* (non-Javadoc)
		 * @see android.text.TextWatcher#beforeTextChanged(java.lang.CharSequence, int, int, int)
		 */
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see android.text.TextWatcher#onTextChanged(java.lang.CharSequence, int, int, int)
		 */
		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
			// TODO Auto-generated method stub
			
		}
		
	}
}
