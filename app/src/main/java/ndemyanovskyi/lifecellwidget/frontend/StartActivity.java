package ndemyanovskyi.lifecellwidget.frontend;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.provider.Telephony;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewFlipper;
import android.widget.ViewSwitcher.ViewFactory;

import java.io.Serializable;
import java.util.Objects;
import java.util.regex.Pattern;

import br.com.sapereaude.maskedEditText.MaskedEditText;
import ndemyanovskyi.lifecellwidget.R;
import ndemyanovskyi.lifecellwidget.app.Permissions;
import ndemyanovskyi.lifecellwidget.app.Values;
import ndemyanovskyi.lifecellwidget.backend.UpdateService;
import ndemyanovskyi.lifecellwidget.backend.lifecell.api.Account;
import ndemyanovskyi.lifecellwidget.backend.lifecell.api.Lifecell;
import ndemyanovskyi.lifecellwidget.backend.lifecell.api.LifecellException.ResponseType;
import ndemyanovskyi.lifecellwidget.backend.lifecell.api.LifecellException;

public class StartActivity extends AppCompatActivity {

    private static final String STATE_STATE = "state";
    private static final String STATE_PERMISSIONS_ALREADY_REQUESTED = "permissions_already_requested";
    private static final String STATE_SETTINGS_OPENED_FOR_GRANT_PERMISSIONS = "settings_opened_for_grant_permissions";

    public static final String EXTRA_STATE = "state";

    /*private static final Pattern PHONE_NUMBER_PATTERN = Pattern.compile("\\+(380\\d{2})-(\\d{3})-(\\d{2})-(\\d{2})");
    private static final Pattern LIFECELL_PHONE_NUMBER_PATTERN = Pattern.compile("\\+(380[976]3)-(\\d{3})-(\\d{2})-(\\d{2})");
    private static final Pattern PRE_LIFECELL_PHONE_NUMBER_PATTERN = Pattern.compile("\\+380([976](3[\\s\\S]*|)|)");*/

    private static final Pattern RAW_PHONE_NUMBER_PATTERN = Pattern.compile("380\\d{9}");
    private static final Pattern RAW_LIFECELL_PHONE_NUMBER_PATTERN = Pattern.compile("380[976]3\\d{7}");
    private static final Pattern RAW_PRE_LIFECELL_PHONE_NUMBER_PATTERN = Pattern.compile("380([976](3[\\s\\S]*|)|)");

    private static final Pattern SHORT_RAW_PHONE_NUMBER_PATTERN = Pattern.compile("\\d{9}");
    private static final Pattern SHORT_RAW_LIFECELL_PHONE_NUMBER_PATTERN = Pattern.compile("[976]3\\d{7}");
    private static final Pattern SHORT_RAW_PRE_LIFECELL_PHONE_NUMBER_PATTERN = Pattern.compile("([976](3[\\s\\S]*|)|)");

    private static final Pattern SUPERPASSWORD_PATTERN = Pattern.compile("(\\d{6})");

    private static final Typeface ROBOTO_THIN_TYPEFACE = Typeface.create("sans-serif-thin", Typeface.NORMAL);

    private static final int PERMISSIONS_REQUEST_CODE = 0;

    private LinearLayout layoutRoot;
    private ImageSwitcher imageSwitcherIcon;
    private TextSwitcher textSwitcherMessage;
    private TextSwitcher textSwitcherRequest;

    private LinearLayout layoutSignIn;
    private TextInputLayout layoutPhoneNumber;
    private TextInputLayout layoutSuperpassword;
    private ViewFlipper viewFlipperRequest;
    private MaskedEditText editTextPhoneNumber;
    private EditText editTextSuperpassword;
    private Button buttonRecallSuperpassword;

    private boolean permissionsAlreadyRequested = false;
    private boolean settingsOpenedForGrantPermissions = false;

    private LoginTask loginTask;

    private State initState;
    private State state;

    @Override
    @SuppressLint("ClickableViewAccessibility")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        layoutRoot = findViewById(R.id.layout_content);

        buttonRecallSuperpassword = findViewById(R.id.button_recall_superpassword);
        buttonRecallSuperpassword.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, "PAROL");
                intent.putExtra("address", "123");

                String defaultSmsPackageName = Telephony.Sms.getDefaultSmsPackage(StartActivity.this);
                if (defaultSmsPackageName != null) {
                    intent.setPackage(defaultSmsPackageName);
                }
                startActivity(intent);
            }
        });
        imageSwitcherIcon = findViewById(R.id.imageswitcher_icon);
        imageSwitcherIcon.setFactory(new ViewFactory() {
            @Override
            public View makeView() {
                ImageView view = new ImageView(getApplicationContext());
                view.setColorFilter(android.R.color.white);
                view.setLayoutParams(new ImageSwitcher.LayoutParams(
                        LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
                view.setScaleType(ScaleType.FIT_XY);
                return view;
            }
        });

        textSwitcherMessage = findViewById(R.id.textswitcher_message);
        textSwitcherMessage.setFactory(new ViewFactory() {
            @Override
            public View makeView() {
                TextView view = new TextView(getApplicationContext());
                view.setTextColor(getResources().getColor(android.R.color.white, getTheme()));
                view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
                view.setTypeface(ROBOTO_THIN_TYPEFACE);
                view.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                return view;
            }
        });
        textSwitcherRequest = findViewById(R.id.textswitcher_request);
        textSwitcherRequest.setFactory(new ViewFactory() {
            @Override
            public View makeView() {
                TextView view = new TextView(getApplicationContext());
                view.setTextColor(getResources().getColor(android.R.color.white, getTheme()));
                view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
                view.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                view.setTypeface(ROBOTO_THIN_TYPEFACE);
                view.setAllCaps(true);
                return view;
            }
        });
        layoutPhoneNumber = findViewById(R.id.layout_phone_number);
        layoutSuperpassword = findViewById(R.id.layout_superpassword);
        layoutSignIn = findViewById(R.id.layout_sign_in);
        viewFlipperRequest = findViewById(R.id.viewflipper_request);
        editTextPhoneNumber = findViewById(R.id.edittext_phone_number);
        editTextPhoneNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (loginTask != null) {
                    loginTask.cancel(true);
                } else {
                    setState(State.SIGN_IN_REQUEST);
                }
                preValidatePhoneNumberEdit();
            }
        });
        editTextPhoneNumber.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    validatePhoneNumberEdit();
                }
            }
        });
        editTextPhoneNumber.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (loginTask != null) {
                        loginTask.cancel(true);
                    } else {
                        setState(State.SIGN_IN_REQUEST);
                    }
                }
                return false;
            }
        });
        editTextSuperpassword = findViewById(R.id.edittext_superpassword);
        editTextSuperpassword.setTransformationMethod(new PasswordTransformationMethod());
        editTextSuperpassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (loginTask != null) {
                    loginTask.cancel(true);
                } else {
                    setState(State.SIGN_IN_REQUEST);
                }
                if (layoutSuperpassword.getError() != null) {
                    validateSuperpasswordEdit();
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
        editTextSuperpassword.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    validateSuperpasswordEdit();
                }
            }
        });
        editTextSuperpassword.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (loginTask != null) {
                        loginTask.cancel(true);
                    } else {
                        setState(State.SIGN_IN_REQUEST);
                    }
                }
                return false;
            }
        });

        Intent intent = getIntent();
        if (intent != null) {
            Serializable serializable = intent.getSerializableExtra(EXTRA_STATE);
            if (serializable instanceof State) {
                initState = (State) serializable;
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle saveInstanceState) {
        super.onSaveInstanceState(saveInstanceState);
        saveInstanceState.putSerializable(STATE_STATE, state);
        saveInstanceState.putBoolean(STATE_PERMISSIONS_ALREADY_REQUESTED, permissionsAlreadyRequested);
        saveInstanceState.putBoolean(STATE_SETTINGS_OPENED_FOR_GRANT_PERMISSIONS, settingsOpenedForGrantPermissions);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        state = (State) savedInstanceState.getSerializable(STATE_STATE);
        permissionsAlreadyRequested = savedInstanceState.getBoolean(STATE_PERMISSIONS_ALREADY_REQUESTED);
        settingsOpenedForGrantPermissions = savedInstanceState.getBoolean(STATE_SETTINGS_OPENED_FOR_GRANT_PERMISSIONS);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(initState != null) {
            setState(initState, true);
            initState = null;
        } else {
            if(settingsOpenedForGrantPermissions) {
                settingsOpenedForGrantPermissions = false;
                Permissions.State state = Permissions.state(this);
                switch (state) {
                    case GRANTED:
                        setState(State.PERMISSIONS_SUCCESS, true);
                        break;
                    case DENIED:
                        if (permissionsAlreadyRequested) {
                            setState(State.PERMISSIONS_REQUEST_AGAIN);
                        } else {
                            setState(State.PERMISSIONS_REQUEST);
                        }
                        break;
                    case FOREVER_DENIED:
                        setState(State.PERMISSIONS_FAILURE);
                        break;
                    default:
                        throw new IllegalStateException(
                                "Unknown pemissions state = " + state);
                }
            } else if(state == null) {
                Permissions.State state = Permissions.state(this);
                switch (state) {
                    case GRANTED:
                        if(Values.hasAccount(this)) {
                            startSettingsActivityAndFinish();
                        } else {
                            setState(State.SIGN_IN_REQUEST);
                        }
                        break;
                    case DENIED:
                        if (permissionsAlreadyRequested) {
                            setState(State.PERMISSIONS_REQUEST_AGAIN);
                        } else {
                            setState(State.PERMISSIONS_REQUEST);
                        }
                        break;
                    case FOREVER_DENIED:
                        setState(State.PERMISSIONS_FAILURE);
                        break;
                    default:
                        throw new IllegalStateException(
                                "Unknown pemissions state = " + state);
                }
            } else {
                setState(state, true);
            }
        }
    }

    private void startSettingsActivityAndFinish() {
        UpdateService.update(this);

        Intent intent = new Intent(this, SettingsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        Values.setPermissionsOnceRequested(this, true);

        if(grantResults.length == Permissions.ARRAY.length) {
            boolean granted = true;
            for (int i = 0; i < grantResults.length; i++) {
                int grantResult = grantResults[i];
                if(grantResult != PackageManager.PERMISSION_GRANTED) {
                    if(shouldShowRequestPermissionRationale(permissions[i])) {
                        setState(State.PERMISSIONS_REQUEST_AGAIN);
                    } else {
                        setState(State.PERMISSIONS_FAILURE);
                    }
                    granted = false;
                    break;
                }
            }
            if(granted) {
                setState(State.PERMISSIONS_SUCCESS);
            }
        }
    }

    public void setState(State state) {
        setState(state, false);
    }

    private void setState(State state, boolean force) {
        state.apply(this, force);
    }

    public void setState(State state, Object... args) {
        setState(state, false, args);
    }

    private void setState(State state, boolean force, Object... args) {
        state.apply(this, force, args);
    }

    public State getState() {
        return state;
    }

    public static boolean startIfNeeded(Context context) {
        if(!Permissions.allGranted(context) || !Values.hasAccount(context)) {
            Intent intent = new Intent(
                    context, StartActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } else {
            return false;
        }
    }

    private boolean validatePhoneNumberEdit() {
        if (!SHORT_RAW_PHONE_NUMBER_PATTERN.matcher(editTextPhoneNumber.getRawText()).matches()) {
            layoutPhoneNumber.setError(ResponseType.INVALID_NUMBER.getMessage(this));
            return false;
        } else {
            if(!SHORT_RAW_LIFECELL_PHONE_NUMBER_PATTERN.matcher(editTextPhoneNumber.getRawText()).matches()) {
                layoutPhoneNumber.setError(ResponseType.OTHER_CARRIER_NUMBER.getMessage(this));
                return false;
            } else {
                layoutPhoneNumber.setError(null);
                return true;
            }
        }
    }

    private boolean preValidatePhoneNumberEdit() {
        if(!SHORT_RAW_PRE_LIFECELL_PHONE_NUMBER_PATTERN.matcher(editTextPhoneNumber.getRawText()).matches()) {
            layoutPhoneNumber.setError(ResponseType.OTHER_CARRIER_NUMBER.getMessage(this));
            return false;
        } else {
            layoutPhoneNumber.setError(null);
            return true;
        }
    }

    private boolean validateSuperpasswordEdit() {
        if (!SUPERPASSWORD_PATTERN.matcher(
                editTextSuperpassword.getText().toString()).matches()) {
            layoutSuperpassword.setError(ResponseType.INVALID_SUPERPASSWORD.getMessage(this));
            return false;
        } else {
            layoutSuperpassword.setError(null);
            return true;
        }
    }

    private void performSignIn() {
        if(validatePhoneNumberEdit() & validateSuperpasswordEdit()) {
            loginTask = new LoginTask();
            loginTask.execute();
        }
    }

    @SuppressLint("StaticFieldLeak")
    public class LoginTask extends AsyncTask<Void, Void, ResponseType> {

        private final String phoneNumber;
        private final String superpassword;

        LoginTask() {
            this.phoneNumber = "380" + editTextPhoneNumber.getRawText();
            this.superpassword = editTextSuperpassword.getText().toString();
        }

        @Override
        protected void onPreExecute() {
            setState(State.SIGN_IN_PROGRESS);
        }

        @Override
        protected ResponseType doInBackground(Void... params) {
            try {
                if(RAW_PHONE_NUMBER_PATTERN.matcher(phoneNumber).matches()) {
                    if(RAW_LIFECELL_PHONE_NUMBER_PATTERN.matcher(phoneNumber).matches()) {
                        Account account = Lifecell.signIn(phoneNumber, superpassword);
                        Values.setAccount(StartActivity.this, account);
                        return ResponseType.SUCCESS;
                    } else {
                        return ResponseType.OTHER_CARRIER_NUMBER;
                    }
                } else {
                    return ResponseType.INVALID_NUMBER;
                }
            } catch (LifecellException e) {
                return e.getResponseType();
            }
        }

        @Override
        protected void onPostExecute(final ResponseType responseType) {
            loginTask = null;
            if (responseType == ResponseType.SUCCESS) {
                setState(State.SIGN_IN_SUCCESS);
                WidgetProvider.refresh(StartActivity.this);
                UpdateService.scheduleNextUpdate(
                        StartActivity.this, UpdateService.SHORT_UPDATE_DURATION);
            } else {
                setState(State.SIGN_IN_FAILURE, responseType);
            }
        }

        @Override
        protected void onCancelled() {
            loginTask = null;
            setState(State.SIGN_IN_REQUEST);
        }
    }

    public enum State {
        /*SPLASH(Factor.SIGN_IN) {
            @Override
            protected void applyInternal(final StartActivity activity, Object... args) {
                animateBackgroundTo(activity, R.color.background_splash);
                activity.imageSwitcherIcon.setImageResource(R.drawable.ic_splash);
                activity.textSwitcherMessage.setText(activity.getText(R.string.info_message_splash));
                activity.textSwitcherRequest.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(Permissions.allGranted(activity)) {
                            if (Values.hasAccount(activity)) {
                                Intent intent = new Intent(activity, SettingsActivity.class);
                                activity.startActivity(intent);
                            } else {
                                activity.setState(SIGN_IN_REQUEST);
                            }
                        } else {
                            activity.setState(PERMISSIONS_REQUEST);
                        }
                    }
                });
                activity.textSwitcherRequest.setText(activity.getText(R.string.action_continue));
                activity.viewFlipperRequest.setDisplayedChild(0);
                activity.layoutSignIn.setVisibility(View.GONE);
            }
        }, */SIGN_IN_REQUEST() {
            @Override
            protected void applyInternal(final StartActivity activity, Object... args) {
                animateBackgroundTo(activity, R.color.background_sign_in_request);
                activity.layoutPhoneNumber.setError(null);
                activity.layoutSuperpassword.setError(null);
                activity.imageSwitcherIcon.setImageResource(R.drawable.ic_sign_in_request);
                activity.textSwitcherMessage.setText(activity.getText(R.string.info_message_sign_in_request));
                activity.textSwitcherRequest.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        activity.performSignIn();
                    }
                });
                activity.textSwitcherRequest.setText(activity.getText(R.string.sign_in));
                activity.viewFlipperRequest.setDisplayedChild(0);
                activity.layoutSignIn.setVisibility(View.VISIBLE);
            }
        }, SIGN_IN_PROGRESS() {
            @Override
            protected void applyInternal(final StartActivity activity, Object... args) {
                animateBackgroundTo(activity, R.color.background_sign_in_progress);
                activity.editTextPhoneNumber.setError(null);
                activity.editTextSuperpassword.setError(null);
                activity.imageSwitcherIcon.setImageResource(R.drawable.ic_sign_in_request);
                activity.textSwitcherMessage.setText(activity.getText(R.string.info_message_sign_in_progress));
                activity.textSwitcherRequest.setOnClickListener(null);
                activity.viewFlipperRequest.setDisplayedChild(1);
                activity.layoutSignIn.setVisibility(View.GONE);
            }
        }, SIGN_IN_FAILURE() {
            @Override
            protected void applyInternal(final StartActivity activity, Object... args) {
                ResponseType responseType = Objects.requireNonNull((ResponseType) args[0], "responseType");
                animateBackgroundTo(activity, R.color.background_sign_in_failure);
                activity.imageSwitcherIcon.setImageResource(R.drawable.ic_sign_in_failure);
                activity.textSwitcherMessage.setText(activity.getText(R.string.info_message_sign_in_failure));
                activity.textSwitcherRequest.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        activity.loginTask = activity.new LoginTask();
                        activity.loginTask.execute();
                    }
                });
                activity.textSwitcherRequest.setText(activity.getText(R.string.sign_in));
                activity.viewFlipperRequest.setDisplayedChild(0);
                activity.layoutSignIn.setVisibility(View.VISIBLE);

                CharSequence message;
                CharSequence phoneNumberError = null;
                CharSequence superpasswordError = null;

                switch (responseType) {
                    case WRONG_NUMBER:
                    case INVALID_NUMBER:
                        message = activity.getText(R.string.message_enter_correct_values);
                        phoneNumberError = responseType.getMessage(activity);
                        break;
                    case WRONG_SUPERPASSWORD:
                    case INVALID_SUPERPASSWORD:
                    case SUPERPASSWORD_LOCKED:
                    case OTHER_CARRIER_NUMBER:
                        message = activity.getText(R.string.message_enter_correct_values);
                        superpasswordError = responseType.getMessage(activity);
                        break;
                    default:
                        message = responseType.getMessage(activity);
                        break;
                }
                activity.layoutPhoneNumber.setError(phoneNumberError);
                activity.layoutSuperpassword.setError(superpasswordError);
                activity.textSwitcherMessage.setText(message);
            }
        }, SIGN_IN_SUCCESS() {
            @Override
            protected void applyInternal(final StartActivity activity, Object... args) {
                animateBackgroundTo(activity, R.color.background_sign_in_success);
                activity.layoutPhoneNumber.setError(null);
                activity.layoutSuperpassword.setError(null);
                activity.imageSwitcherIcon.setImageResource(R.drawable.ic_sign_in_success);
                activity.textSwitcherMessage.setText(activity.getText(R.string.info_message_sign_in_success));
                activity.textSwitcherRequest.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(Permissions.allGranted(activity)) {
                            activity.startSettingsActivityAndFinish();
                        } else {
                            activity.setState(PERMISSIONS_REQUEST);
                        }
                    }
                });
                activity.textSwitcherRequest.setText(activity.getText(R.string.action_continue));
                activity.viewFlipperRequest.setDisplayedChild(0);
                activity.layoutSignIn.setVisibility(View.GONE);
            }
        }, PERMISSIONS_REQUEST() {
            @Override
            protected void applyInternal(final StartActivity activity, Object... args) {
                animateBackgroundTo(activity, R.color.background_permissions_request);
                activity.imageSwitcherIcon.setImageResource(R.drawable.ic_emoticon_happy);
                activity.textSwitcherMessage.setText(activity.getText(R.string.info_message_permission_request));
                activity.textSwitcherRequest.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        activity.permissionsAlreadyRequested = true;
                        ActivityCompat.requestPermissions(
                                activity, Permissions.ARRAY, PERMISSIONS_REQUEST_CODE);
                    }
                });
                activity.textSwitcherRequest.setText(activity.getText(R.string.action_request));
                activity.viewFlipperRequest.setDisplayedChild(0);
                activity.layoutSignIn.setVisibility(View.GONE);
            }
        }, PERMISSIONS_REQUEST_AGAIN() {
            @Override
            protected void applyInternal(final StartActivity activity, Object... args) {
                animateBackgroundTo(activity, R.color.background_permissions_request_again);
                activity.imageSwitcherIcon.setImageResource(R.drawable.ic_emoticon_sad);
                activity.textSwitcherMessage.setText(activity.getText(R.string.info_message_permission_failure));
                activity.textSwitcherRequest.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        activity.permissionsAlreadyRequested = true;
                        ActivityCompat.requestPermissions(
                                activity, Permissions.ARRAY, PERMISSIONS_REQUEST_CODE);
                    }
                });
                activity.textSwitcherRequest.setText(activity.getText(R.string.action_request_again));
                activity.viewFlipperRequest.setDisplayedChild(0);
                activity.layoutSignIn.setVisibility(View.GONE);
            }
        }, PERMISSIONS_FAILURE() {
            @Override
            protected void applyInternal(final StartActivity activity, Object... args) {
                animateBackgroundTo(activity, R.color.background_permissions_failure);
                activity.imageSwitcherIcon.setImageResource(R.drawable.ic_emoticon_sad);
                activity.textSwitcherMessage.setText(activity.getText(R.string.info_message_permission_failure));
                activity.textSwitcherRequest.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.addCategory(Intent.CATEGORY_DEFAULT);
                        intent.setData(Uri.parse("package:" + activity.getPackageName()));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                        activity.startActivity(intent);
                        activity.settingsOpenedForGrantPermissions = true;
                    }
                });
                activity.textSwitcherRequest.setText(activity.getText(R.string.action_go_to_settings));
                activity.viewFlipperRequest.setDisplayedChild(0);
                activity.layoutSignIn.setVisibility(View.GONE);
            }
        }, PERMISSIONS_SUCCESS() {
            @Override
            protected void applyInternal(final StartActivity activity, Object... args) {
                animateBackgroundTo(activity, R.color.background_permissions_success);
                activity.imageSwitcherIcon.setImageResource(R.drawable.ic_emoticon_cool);
                activity.textSwitcherMessage.setText(activity.getText(R.string.info_message_permission_success));
                activity.textSwitcherRequest.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(Values.hasAccount(activity)) {
                            activity.startSettingsActivityAndFinish();
                        } else {
                            activity.setState(SIGN_IN_REQUEST);
                        }
                    }
                });
                activity.textSwitcherRequest.setText(activity.getText(R.string.action_continue));
                activity.viewFlipperRequest.setDisplayedChild(0);
                activity.layoutSignIn.setVisibility(View.GONE);
            }
        };

        private static final Object[] EMPTY_ARGS = new Object[0];

        public final void apply(final StartActivity activity) {
            apply(activity, false, EMPTY_ARGS);
        }

        public final void apply(final StartActivity activity, Object... args) {
            apply(activity, false, args);
        }

        private final void apply(final StartActivity activity, boolean force) {
            apply(activity, force, EMPTY_ARGS);
        }

        private final void apply(final StartActivity activity, boolean force, Object... args) {
            if(!Looper.getMainLooper().isCurrentThread()) {
                throw new IllegalStateException(
                        "Method 'apply' must be running only on a main thread.");
            }
            if(force) {
                applyInternal(activity, args);
                activity.state = this;
            } else if(activity.state != this) {
                applyInternal(activity, args);
                activity.state = this;
            }
        }

        protected abstract void applyInternal(final StartActivity activity, Object... args);

        private static void animateBackgroundTo(final StartActivity activity, int backgroundColorResId) {
            int backgroundColor = activity.getColor(backgroundColorResId);
            if(activity.state != null) {
                final Drawable background = activity.layoutRoot.getBackground();
                final int oldBackgroundColor;
                if(background instanceof ColorDrawable) {
                    oldBackgroundColor = ((ColorDrawable) background).getColor();
                } else {
                    throw new IllegalStateException(
                            "Root background must be only ColorDrawable.");
                }
                ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), oldBackgroundColor, backgroundColor);
                colorAnimation.setDuration(1000);
                colorAnimation.addUpdateListener(new AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animator) {
                        activity.layoutRoot.setBackgroundColor((int) animator.getAnimatedValue());
                    }
                });
                colorAnimation.start();
            } else {
                activity.layoutRoot.setBackgroundColor(backgroundColor);
            }
        }
    }
}
