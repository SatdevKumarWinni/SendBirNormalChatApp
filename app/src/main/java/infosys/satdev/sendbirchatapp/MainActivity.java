package infosys.satdev.sendbirchatapp;


import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sendbird.android.ApplicationUserListQuery;
import com.sendbird.android.BaseChannel;
import com.sendbird.android.BaseMessage;
import com.sendbird.android.GroupChannel;
import com.sendbird.android.GroupChannelListQuery;
import com.sendbird.android.Member;
import com.sendbird.android.OpenChannel;
import com.sendbird.android.PreviousMessageListQuery;
import com.sendbird.android.SendBird;
import com.sendbird.android.SendBirdException;
import com.sendbird.android.User;
import com.sendbird.android.UserListQuery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    LinearLayout llUserLogin, llLogedUser;
    EditText UserId, Username,etChatUserId;
    Button userLogin, btDone, btStartChat;
    TextView tvUserId, tvUserName;
    RecyclerView rvMyFriendList;
    private GroupChannelListQuery mChannelListQuery;
    private ApplicationUserListQuery mUserListQuery;
    private GroupChannel mChannel;
    private PreviousMessageListQuery mPrevMessageListQuery;

    public static String getGroupChannelTitle(GroupChannel channel) {
        List<Member> members = channel.getMembers();

        if (members.size() < 2 || SendBird.getCurrentUser() == null) {
            return "No Members";
        } else if (members.size() == 2) {
            StringBuffer names = new StringBuffer();
            for (Member member : members) {
                if (member.getUserId().equals(SendBird.getCurrentUser().getUserId())) {
                    continue;
                }

                names.append(", " + member.getNickname());
            }
            return names.delete(0, 2).toString();
        } else {
            int count = 0;
            StringBuffer names = new StringBuffer();
            for (User member : members) {
                if (member.getUserId().equals(SendBird.getCurrentUser().getUserId())) {
                    continue;
                }

                count++;
                names.append(", " + member.getNickname());

                if (count >= 10) {
                    break;
                }
            }
            return names.delete(0, 2).toString();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (SendBird.getConnectionState() == SendBird.ConnectionState.OPEN) {
            Toast.makeText(this, "Send Bird Is Initallition Done", Toast.LENGTH_SHORT).show();
        }
        UserId = findViewById(R.id.etUserId);
        Username = findViewById(R.id.etUserName);
        llUserLogin = findViewById(R.id.llUserDetails);
        tvUserId = findViewById(R.id.tvUserId);
        tvUserName = findViewById(R.id.tvUserName);
        llLogedUser = findViewById(R.id.llLogedUser);
        userLogin = findViewById(R.id.btLogin);
        etChatUserId = findViewById(R.id.etChatUserId);
        rvMyFriendList = findViewById(R.id.rvMyFriendList);
        rvMyFriendList.setLayoutManager(new LinearLayoutManager(this));


        btDone = findViewById(R.id.btDone);
        btStartChat = findViewById(R.id.btStartChat);
        btStartChat.setOnClickListener(this);
        btDone.setOnClickListener(this);
        userLogin.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btDone:
                sendBirdInit();
                break;
            case R.id.btLogin:
                userLoginClick();
                break;
            case R.id.btStartChat:
                startChat();
                break;
        }
    }

    public void sendBirdInit() {
        // Initialize SendBird instance to use APIs in your app.
        SendBird.init("A2305BC6-FA8D-47D9-8393-30A7AB757CD0", getApplicationContext());
        llUserLogin.setVisibility(View.VISIBLE);
        btDone.setVisibility(View.GONE);


    }

    public void userLoginClick() {
        loginUser(UserId.getText().toString().trim(),
                Username.getText().toString().trim());

    }

    // create user or login user
    public void loginUser(String UserId, String username) {
        if (UserId.isEmpty()) return;
        if (username.isEmpty()) return;
        SendBird.connect(UserId, (user, e) -> {

            myFriendList(15);

            llUserLogin.setVisibility(View.GONE);
            llLogedUser.setVisibility(View.VISIBLE);
            tvUserId.setText("User Id : " + user.getUserId());
            updateCurrentUserInfo(username);
            if (e != null) {
                // Handle error.
            }

        });

    }

    /**
     * Updates the user's nickname.
     *
     * @param userNickname The new nickname of the user.
     */
    private void updateCurrentUserInfo(final String userNickname) {
        SendBird.updateCurrentUserInfo(userNickname, "www.satdevkumar.com", new SendBird.UserInfoUpdateHandler() {
            @Override
            public void onUpdated(SendBirdException e) {
                if (e != null) {
                    // Error!
                    Toast.makeText(
                            MainActivity.this, "" + e.getCode() + ":" + e.getMessage(),
                            Toast.LENGTH_SHORT)
                            .show();

                    // Show update failed snackbar
//                    showSnackbar("Update user nickname failed");

                    return;
                }

                tvUserName.setText("User Name : " + userNickname);
            }
        });
    }

    private void myFriendList(int numChannels) {
        mChannelListQuery = GroupChannel.createMyGroupChannelListQuery();
        mChannelListQuery.setLimit(numChannels);

        mChannelListQuery.next(new GroupChannelListQuery.GroupChannelListQueryResultHandler() {
            @Override
            public void onResult(List<GroupChannel> list, SendBirdException e) {
                if (e != null) {
                    // Error!
                    e.printStackTrace();
                    return;
                }
                for (int i = 0; i < list.size(); i++) {
                    Log.e("data", "url Link: " + list.get(i).getUrl());

                }

//                enterChannel(list.get(1).getUrl());
//                rvMyFriendList.setAdapter(new MyFriendListAdapter(MainActivity.this, list));

            }
        });


    }

    public void startChat() {

        //create group channel or get channel
        List<String> list = new ArrayList();
        list.add(etChatUserId.getText().toString().trim());
        createGroupChannel(list,true);

    }

/*
    public void createGroupChannel(List<String> userIds, boolean distinct) {
        GroupChannel.createChannelWithUserIds(userIds, distinct, (groupChannel, e) -> {
            if (e != null) {
                // Error!
                return;
            }
            enterChannel(groupChannel.getUrl());

        });
    }*/

    /**
     * Enters a Group Channel with a URL.
     *
     * @param channelUrl The URL of the channel to enter.
     */
    private void enterChannel(String channelUrl) {
        GroupChannel.getChannel(channelUrl, new GroupChannel.GroupChannelGetHandler() {
            @Override
            public void onResult(GroupChannel groupChannel, SendBirdException e) {
                if (e != null) {
                    // Error!
                    e.printStackTrace();

                    return;
                }

                mChannel=groupChannel;
                // Enter the channel
                groupChannel.endTyping();
                loadPreviousMessages(20,null);
//                getMessageListData();


            }
        });



    }

//    private void getMessageListData() {
//        mChannel.getPreviousMessagesByTimestamp(oldestMessageCreatedAt, false, limit, true, BaseChannel.MessageTypeFilter.ALL, null, new BaseChannel.GetMessagesHandler() {
//            @Override
//            public void onResult(List<BaseMessage> list, SendBirdException e) {
//                if(handler != null) {
//                    handler.onResult(list, e);
//                }
//
//                setMessageListLoading(false);
//                if(e != null) {
//                    e.printStackTrace();
//                    return;
//                }
//
//                for(BaseMessage message : list) {
//                    mMessageList.add(message);
//                }
//
//                notifyDataSetChanged();
//            }
//        });
//
//    }

//    private void loadInitialMessageList() {
//
//        mPrevMessageListQuery = mChannel.getpreviousmessage();
//        mPrevMessageListQuery.load(30, true
//                , new PreviousMessageListQuery.MessageListQueryResult() {
//                    @Override
//                    public void onResult(List<BaseMessage> list, SendBirdException e) {
//                        if (e != null) {
//                            // Error!
//                            e.printStackTrace();
//                            return;
//                        }
//                        for (int i = 0; i < list.size(); i++) {
//
//                            Log.e("messageData", "onResult: " + list.get(i).getMessage());
//                            Log.e("messageData", "onResult: " + list.get(i).getData());
//                        }
//
//                    }
//                });
//
//    }

    /**
     * Load old message list.
     * @param limit
     * @param handler
     */
    public void loadPreviousMessages(int limit, final BaseChannel.GetMessagesHandler handler) {
        if (mChannel == null) {
            return;
        }

//        if(isMessageListLoading()) {
//            return;
//        }

        long oldestMessageCreatedAt = Long.MAX_VALUE;
        mMessageList=new ArrayList<>();
        if(mMessageList.size() > 0) {
            oldestMessageCreatedAt = mMessageList.get(mMessageList.size() - 1).getCreatedAt();
        }

//        setMessageListLoading(true);
        mChannel.getPreviousMessagesByTimestamp(oldestMessageCreatedAt,
                false, limit, true, BaseChannel.MessageTypeFilter.ALL,
                null, new BaseChannel.GetMessagesHandler() {
            @Override
            public void onResult(List<BaseMessage> list, SendBirdException e) {
                if(handler != null) {
                    handler.onResult(list, e);
                }

//                setMessageListLoading(false);
                if(e != null) {
                    e.printStackTrace();
                    return;
                }

                mMessageList.clear();
                for(BaseMessage message : list) {
                    mMessageList.add(message);
                }
                for (int i = 0; i < mMessageList.size(); i++) {
                    Log.e("message", "onResult: "+mMessageList.get(i).getMessage() );
                    Log.e("message", "onResult: "+mMessageList.get(i).getCustomType() );
                    Log.e("message", "onResult: "+mMessageList.get(i).getCreatedAt() );


                }
                Collections.reverse(mMessageList);

                rvMyFriendList.setAdapter(new ChatAdapter(MainActivity.this,mMessageList));


//                notifyDataSetChanged();
            }
        });
    }

    private List<BaseMessage> mMessageList;


    private void loadInitialUserList(int size) {
        mUserListQuery = SendBird.createApplicationUserListQuery();

        mUserListQuery.setLimit(size);
        mUserListQuery.next(new UserListQuery.UserListQueryResultHandler() {
            @Override
            public void onResult(List<User> list1, SendBirdException e) {
                if (e != null) {
                    // Error!
                    return;
                }
                List list = new ArrayList();
                for (int i = 0; i < list1.size(); i++) {
                    Log.e("TotalUserListName", "onResult: " + list1.get(i).getUserId());

                    list.add(list1.get(i).getUserId());

                }
                Log.e("TotalUserListName", "onResult: " + list.size());

//                mListAdapter.setUserList(list);
            }
        });
    }

    public void createGroupChannel(List<String> userIds, boolean distinct) {
        GroupChannel.createChannelWithUserIds(userIds, distinct, (groupChannel, e) -> {
            if (e != null) {
                // Error!
                return;
            }
            Intent intent=new Intent(this ,ChatLayout.class);
            intent.putExtra("channlUrl",groupChannel.getUrl());
            intent.putExtra("LoginUserId",tvUserId.getText().toString().trim());
            startActivity(intent);



        });
    }
}

