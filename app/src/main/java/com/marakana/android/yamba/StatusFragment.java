package com.marakana.android.yamba;

import java.util.Locale;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.marakana.android.yamba.clientlib.YambaClient;

public class StatusFragment extends Fragment implements OnInitListener {
	private static final String TAG = StatusFragment.class.getSimpleName();
	private static final String PROVIDER = LocationManager.GPS_PROVIDER;
	private Button mButtonTweet;
	private EditText mTextStatus;
	private TextView mTextCount;
	private int mDefaultColor;
	private LocationManager locationManager;
	private static Location location;

	private TextToSpeech mTts; // clk: TextToSpeech to say status-text

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		locationManager = (LocationManager) getActivity().getSystemService(
				Context.LOCATION_SERVICE);
		location = locationManager.getLastKnownLocation(PROVIDER);
	}

	@Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // clk: TTS needs a Context (or Activity) in constructor, so do this in onActivityCreated()
        mTts = new TextToSpeech(getActivity(), this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // clk: release TTS resources here rather than in onDestroy(), since from here fragment
        //  can be returned to layout from the back stack and does not go through onDestroy()
        if (mTts != null) mTts.shutdown();
    }

    @Override
	public void onResume() {
		super.onResume();
		locationManager.requestLocationUpdates(PROVIDER, 60000, 1000,
				LOCATION_LISTENER);
	}

	@Override
	public void onPause() {
		super.onPause();
		locationManager.removeUpdates(LOCATION_LISTENER);
	}

	private static final LocationListener LOCATION_LISTENER = new LocationListener() {
		@Override
		public void onLocationChanged(Location l) {
			location = l;
		}

		@Override
		public void onProviderDisabled(String provider) {
		}

		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}
	};

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_status, container, false);
		// clk: fix to pass in container instead of null, following
		//  http://developer.android.com/guide/components/fragments.html

		mButtonTweet = (Button) v.findViewById(R.id.status_button_tweet);
		mTextStatus = (EditText) v.findViewById(R.id.status_text);
		mTextCount = (TextView) v.findViewById(R.id.status_text_count);
		mTextCount.setText(Integer.toString(140));
		mDefaultColor = mTextCount.getTextColors().getDefaultColor();

		mButtonTweet.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String status = mTextStatus.getText().toString();
				PostTask postTask = new PostTask();
				postTask.execute(status);
				Log.d(TAG, "onClicked");
			}

		});

		mTextStatus.addTextChangedListener(new TextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
				int count = 140 - s.length();
				mTextCount.setText(Integer.toString(count));

				if (count < 50) {
					mTextCount.setTextColor(Color.RED);
				} else {
					mTextCount.setTextColor(mDefaultColor);
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
			}

		});

		Log.d(TAG, "onCreated");

		return v;
	}

    // clk: TextToSpeech initialization callback
    @Override
    public void onInit(int status) {
        if (status==TextToSpeech.SUCCESS) {
            int retCode = mTts.setLanguage(Locale.getDefault());
            if (retCode == TextToSpeech.LANG_COUNTRY_AVAILABLE) {
                // good to go
                return;
            } else {
                // missing data for language, so install it
                Intent install = new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(install);
            }
        } else {
            Toast.makeText(getActivity(), "TTS initialization Failed", Toast.LENGTH_LONG).show();
            Log.e(TAG, "onInit() status code " + status + " was Not Success");
        }
    }

    class PostTask extends AsyncTask<String, Void, String> {
		private ProgressDialog progress;

		@Override
		protected void onPreExecute() {
			progress = ProgressDialog.show(getActivity(), "Posting",
					"Please wait...");
			progress.setCancelable(true);
		}

		// Executes on a non-UI thread
		@Override
		protected String doInBackground(String... params) {
			try {
				SharedPreferences prefs = PreferenceManager
						.getDefaultSharedPreferences(getActivity());
				String username = prefs.getString("username", "");
				String password = prefs.getString("password", "");

				// Check that username and password are not empty
				// If empty, Toast a message to set login info and bounce to
				// SettingActivity
				// Hint: TextUtils.
				if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
					getActivity().startActivity(
							new Intent(getActivity(), SettingsActivity.class));
					return "Please update your username and password";
				}

				YambaClient cloud = new YambaClient(username, password,
				        RefreshService.YAMBA_API_ROOT); // clk: instead of always using built-in default api-root
				if (location != null) {
					cloud.postStatus(params[0], location.getLatitude(),
							location.getLongitude());
				} else {
					cloud.postStatus(params[0]);
				}

				Log.d(TAG, "Successfully posted to the cloud: " + params[0]);
				return params[0]; // clk: return the status-text so that we can say it

			} catch (Exception e) {
				Log.e(TAG, "Failed to post to the cloud", e);
				e.printStackTrace();
				return "Failed to post";
			}
		}

		// Called after doInBackground() on UI thread
		@Override
		protected void onPostExecute(String result) {
			progress.dismiss();

            if (mTts != null)
                mTts.speak(result, TextToSpeech.QUEUE_FLUSH, null); // clk: say the status-text posted
            if (getActivity() != null && result != null)
                Toast.makeText(getActivity(), "Successfully posted", Toast.LENGTH_LONG).show();
		}

	}
}
