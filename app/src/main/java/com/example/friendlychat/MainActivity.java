package com.example.friendlychat;

import android.content.Intent;
import android.os.Bundle;

import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.data.model.User;
import com.google.android.gms.auth.api.Auth;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    public static final String ANONYMOUS = "anonymous";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;
    private static final int RC_SIGN_IN=1;

    private ListView mMessageListView;
    private MessageAdapter mMessageAdapter;
    private ProgressBar mProgressBar;
    private ImageButton mPhotoPickerButton;
    private EditText mMessageEditText;
    private Button mSendButton;
    private FirebaseDatabase mfirebaseDatabase;
    private DatabaseReference mdatabasemessageReference;
    private ChildEventListener mchildEventListener;
    private FirebaseAuth mfirebaseAuth;
    private FirebaseAuth.AuthStateListener mauthStateListener;

    private String mUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUsername = ANONYMOUS;
        mfirebaseDatabase=FirebaseDatabase.getInstance();
        mfirebaseAuth=FirebaseAuth.getInstance();
        mdatabasemessageReference=mfirebaseDatabase.getReference().child("message");
        // Initialize references to views
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mMessageListView = (ListView) findViewById(R.id.messageListView);
        mPhotoPickerButton = (ImageButton) findViewById(R.id.photoPickerButton);
        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        mSendButton = (Button) findViewById(R.id.sendButton);

        // Initialize message ListView and its adapter
        List<FriendlyMessage> friendlyMessages = new ArrayList<>();
        mMessageAdapter = new MessageAdapter(this, R.layout.list_item, friendlyMessages);
        mMessageListView.setAdapter(mMessageAdapter);

        // Initialize progress bar
        mProgressBar.setVisibility(ProgressBar.INVISIBLE);

        // ImagePickerButton shows an image picker to upload a image for a message
        mPhotoPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: Fire an intent to show an image picker
            }
        });

        // Enable Send button when there's text to send
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT)});

        // Send button sends a message and clears the EditText
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: Send messages on click
                 FriendlyMessage friendlyMessage=new FriendlyMessage(mMessageEditText.getText().toString(),mUsername.toString(),null);
                 mdatabasemessageReference.push().setValue(friendlyMessage);
                // Clear input box
                mMessageEditText.setText("");
            }
        });
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build());
        mauthStateListener=new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user=firebaseAuth.getCurrentUser();

                if(user!=null)
                {
                  setonSignIN(user.getDisplayName());
                }
                else{
                    setonSignout();
                       startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder().setIsSmartLockEnabled(false).setAvailableProviders(providers).build(),RC_SIGN_IN);
                }
            }
        };
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.sign_out_menu:
                AuthUI.getInstance().signOut(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==RC_SIGN_IN) {
            if(resultCode==RESULT_FIRST_USER)
            Toast.makeText(MainActivity.this, "Signing in", Toast.LENGTH_SHORT).show();

        else if(resultCode==RESULT_CANCELED){
                Toast.makeText(MainActivity.this, "failed Signing in", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mauthStateListener!=null)
        mfirebaseAuth.removeAuthStateListener(mauthStateListener);

        detachDatabaseReadListner();
        mMessageAdapter.clear();
    }
    protected void onResume()
    {
        super.onResume();
        mfirebaseAuth.addAuthStateListener(mauthStateListener);

    }

    private void setonSignIN(String username)
    {
        mUsername=username;
        AttackDatabaseReadListner();
    }
    private void setonSignout()
    {
        mUsername=ANONYMOUS;
        mMessageAdapter.clear();
        detachDatabaseReadListner();
    }
    private void AttackDatabaseReadListner()
    {
       if(mchildEventListener==null) {
           mchildEventListener = new ChildEventListener() {
               @Override
               public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                   FriendlyMessage friendlyMessage = snapshot.getValue(FriendlyMessage.class);
                   mMessageAdapter.add(friendlyMessage);
               }

               @Override
               public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

               }

               @Override
               public void onChildRemoved(@NonNull DataSnapshot snapshot) {

               }

               @Override
               public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

               }

               @Override
               public void onCancelled(@NonNull DatabaseError error) {

               }
           };
           mdatabasemessageReference.addChildEventListener(mchildEventListener);

       }
    }
    private void detachDatabaseReadListner()
    {
        if(mchildEventListener!=null) {
            mdatabasemessageReference.removeEventListener(mchildEventListener);
        mchildEventListener=null;
        }
    }
}
