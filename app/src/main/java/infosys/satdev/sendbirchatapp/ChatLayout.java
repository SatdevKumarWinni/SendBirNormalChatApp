package infosys.satdev.sendbirchatapp;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sendbird.android.BaseChannel;
import com.sendbird.android.BaseMessage;
import com.sendbird.android.GroupChannel;
import com.sendbird.android.Member;
import com.sendbird.android.SendBird;
import com.sendbird.android.SendBirdException;
import com.sendbird.android.UserMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ChatLayout extends AppCompatActivity {
    private static final int CHANNEL_LIST_LIMIT = 30;
    private static final String CONNECTION_HANDLER_ID = "CONNECTION_HANDLER_GROUP_CHAT";
    private static final String CHANNEL_HANDLER_ID = "CHANNEL_HANDLER_GROUP_CHANNEL_CHAT";
    RecyclerView rvChatListData;
    Button btnSend;
    EditText etMessage;
    ChatAdapter chatAdapter;
    String LoginUserId;
    private GroupChannel mChannel;
    private List<BaseMessage> mMessageList;
    private BaseMessage mEditingMessage = null;
    private String mChannelUrl;
    private LinearLayoutManager mLayoutManager;
    private View mCurrentEventLayout;
    private TextView mCurrentEventText;
boolean mIsTyping;
    public static List<String> extractUrls(String input) {
        List<String> result = new ArrayList<String>();

        String[] words = input.split("\\s+");


        Pattern pattern = Patterns.WEB_URL;
        for (String word : words) {
            if (pattern.matcher(word).find()) {
                if (!word.toLowerCase().contains("http://") && !word.toLowerCase().contains("https://")) {
                    word = "http://" + word;
                }
                result.add(word);
            }
        }

        return result;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_layout);
        rvChatListData = findViewById(R.id.rvChatListData);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btSendMessage);
        mCurrentEventLayout = findViewById(R.id.layout_group_chat_current_event);
        mCurrentEventText = findViewById(R.id.text_group_chat_current_event);

        mChannelUrl = getIntent().getStringExtra("channlUrl");
        LoginUserId = getIntent().getStringExtra("LoginUserId");


        //set adapter
        chatAdapter = new ChatAdapter(this);

        // all message lists
        mMessageList = new ArrayList<>();
        chatAdapter.load(mChannelUrl);

        setAdapter();
        btnSend.setOnClickListener(v -> {
            String userInput = etMessage.getText().toString();
            if (userInput.length() > 0) {
                sendUserMessage(userInput);
                etMessage.setText("");
            }
        });

        mIsTyping = false;
        etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!mIsTyping) {
                    setTypingStatus(true);
                }

                if (s.length() == 0) {
                    setTypingStatus(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

    }

    private void sendUserMessage(String text) {
        if (mChannel == null) {
            return;
        }

//        List<String> urls = extractUrls(text);
//        if (urls.size() > 0) {
//            sendUserMessageWithUrl(text, urls.get(0));
//            return;
//        }

        UserMessage tempUserMessage = mChannel.sendUserMessage(text, new BaseChannel.SendUserMessageHandler() {
            @Override
            public void onSent(UserMessage userMessage, SendBirdException e) {
                if (e != null) {
                    // Error!
                    if (getBaseContext() != null) {
                        Toast.makeText(
                                getBaseContext(),
                                "Send failed with error " + e.getCode() + ": " + e.getMessage(), Toast.LENGTH_SHORT)
                                .show();
                    }
                    chatAdapter.markMessageFailed(userMessage);
                    return;
                }

                // Update a sent message to RecyclerView
                chatAdapter.markMessageSent(userMessage);
            }
        });

        // Display a user message to RecyclerView
        chatAdapter.addFirst(tempUserMessage);
    }

    public void createGroupChannel(List<String> userIds, boolean distinct) {
        GroupChannel.createChannelWithUserIds(userIds, distinct, (groupChannel, e) -> {
            if (e != null) {
                // Error!
                return;
            }
            mChannelUrl = groupChannel.getUrl();

            enterChannel(groupChannel.getUrl());
            setAdapter();


        });
    }


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

                mChannel = groupChannel;
                loadLatestMessages(20, new BaseChannel.GetMessagesHandler() {
                    @Override
                    public void onResult(List<BaseMessage> list, SendBirdException e) {
                        markAllMessagesAsRead();
                    }
                });

                // Enter the channel
                groupChannel.endTyping();
                loadPreviousMessages(20, null);


            }
        });


    }


    public void loadPreviousMessages(int limit, final BaseChannel.GetMessagesHandler handler) {
        if (mChannel == null) {
            return;
        }

//        if(isMessageListLoading()) {
//            return;
//        }

        long oldestMessageCreatedAt = Long.MAX_VALUE;
        mMessageList = new ArrayList<>();
        if (mMessageList.size() > 0) {
            oldestMessageCreatedAt = mMessageList.get(mMessageList.size() - 1).getCreatedAt();
        }

//        setMessageListLoading(true);
        mChannel.getPreviousMessagesByTimestamp(oldestMessageCreatedAt,
                false, limit, true, BaseChannel.MessageTypeFilter.ALL,
                null, new BaseChannel.GetMessagesHandler() {
                    @Override
                    public void onResult(List<BaseMessage> list, SendBirdException e) {
                        if (handler != null) {
                            handler.onResult(list, e);
                        }

//                setMessageListLoading(false);
                        if (e != null) {
                            e.printStackTrace();
                            return;
                        }

                        for (BaseMessage message : list) {
                            mMessageList.add(message);
                        }
                        if (mMessageList.size() > 0) {
                            mEditingMessage = mMessageList.get(0);
                        }
                        setAdapter();


//                notifyDataSetChanged();
                    }
                });
    }


    private void editMessage(final BaseMessage message, String editedMessage) {
        if (mChannel == null) {
            return;
        }
        Log.e("dataList", "editMessage: " + message.getMessageId());
        Log.e("dataList", "editMessage: " + message.getMessage());

        mChannel.updateUserMessage(message.getMessageId(), editedMessage, null, null, new BaseChannel.UpdateUserMessageHandler() {
            @Override
            public void onUpdated(UserMessage userMessage, SendBirdException e) {
                if (e != null) {
                    // Error!
                    return;
                }

                loadLatestMessages(20, new BaseChannel.GetMessagesHandler() {
                    @Override
                    public void onResult(List<BaseMessage> list, SendBirdException e) {
                    }
                });
            }
        });
    }

    /**
     * Replaces current message list with new list.
     * Should be used only on initial load or refresh.
     */
    public void loadLatestMessages(int limit, final BaseChannel.GetMessagesHandler handler) {
        if (mChannel == null) {
            return;
        }

//        if(isMessageListLoading()) {
//            return;
//        }

//        setMessageListLoading(true);
        mChannel.getPreviousMessagesByTimestamp(Long.MAX_VALUE, true, limit, true, BaseChannel.MessageTypeFilter.ALL, null, new BaseChannel.GetMessagesHandler() {
            @Override
            public void onResult(List<BaseMessage> list, SendBirdException e) {
                if (handler != null) {
                    handler.onResult(list, e);
                }

//                setMessageListLoading(false);
                if (e != null) {
                    e.printStackTrace();
                    return;
                }

                if (list.size() <= 0) {
                    return;
                }

                for (BaseMessage message : mMessageList) {
                    if (isTempMessage(message) || isFailedMessage(message)) {
                        list.add(0, message);
                    }
                }

                mMessageList.clear();

                for (BaseMessage message : list) {
                    mMessageList.add(message);
                }
                setAdapter();


//                notifyDataSetChanged();
            }
        });
    }

    private void setAdapter() {
        mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setReverseLayout(true);
        rvChatListData.setLayoutManager(mLayoutManager);
        rvChatListData.setAdapter(chatAdapter);
        rvChatListData.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (mLayoutManager.findLastVisibleItemPosition() ==
                        chatAdapter.getItemCount() - 1) {
                    chatAdapter.loadPreviousMessages(CHANNEL_LIST_LIMIT, null);
                }
            }
        });


    }


    /**
     * Notifies that the user has read all (previously unread) messages in the channel.
     * Typically, this would be called immediately after the user enters the chat and loads
     * its most recent messages.
     */
    public void markAllMessagesAsRead() {
        if (mChannel != null) {
            mChannel.markAsRead();
        }
//        notifyAll();
    }

    public boolean isTempMessage(BaseMessage message) {
        return message.getMessageId() == 0;
    }

    public boolean isFailedMessage(BaseMessage message) {
        if (!isTempMessage(message)) {
            return false;
        }

        return message.getSendingStatus() == BaseMessage.SendingStatus.FAILED;
    }


    @Override
    public void onResume() {
        super.onResume();

        ConnectionManager.addConnectionManagementHandler
                (CONNECTION_HANDLER_ID, LoginUserId,
                        new ConnectionManager.ConnectionManagementHandler() {
                            @Override
                            public void onConnected(boolean reconnect) {
                                refresh();
                            }
                        });

//        mChatAdapter.setContext(getActivity()); // Glide bug fix (java.lang.IllegalArgumentException: You cannot start a load for a destroyed activity)

        // Gets channel from URL user requested

//        Log.d(LOG_TAG, mChannelUrl);

        SendBird.addChannelHandler(CHANNEL_HANDLER_ID, new SendBird.ChannelHandler() {
            @Override
            public void onMessageReceived(BaseChannel baseChannel, BaseMessage baseMessage) {
                if (baseChannel.getUrl().equals(mChannelUrl)) {
                    chatAdapter.markAllMessagesAsRead();
                    // Add new message to view
                    chatAdapter.addFirst(baseMessage);
                }
            }

            @Override
            public void onMessageDeleted(BaseChannel baseChannel, long msgId) {
                super.onMessageDeleted(baseChannel, msgId);
                if (baseChannel.getUrl().equals(mChannelUrl)) {
                    chatAdapter.delete(msgId);
                }
            }

            @Override
            public void onMessageUpdated(BaseChannel channel, BaseMessage message) {
                super.onMessageUpdated(channel, message);
                if (channel.getUrl().equals(mChannelUrl)) {
                    chatAdapter.update(message);
                }
            }

            @Override
            public void onReadReceiptUpdated(GroupChannel channel) {
                if (channel.getUrl().equals(mChannelUrl)) {
                    chatAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onTypingStatusUpdated(GroupChannel channel) {
                if (channel.getUrl().equals(mChannelUrl)) {
                    List<Member> typingUsers = channel.getTypingMembers();
                    displayTyping(typingUsers);
                }
            }

            @Override
            public void onDeliveryReceiptUpdated(GroupChannel channel) {
                if (channel.getUrl().equals(mChannelUrl)) {
                    chatAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    @Override
    public void onPause() {
//        setTypingStatus(false);

        ConnectionManager.removeConnectionManagementHandler(CONNECTION_HANDLER_ID);
        SendBird.removeChannelHandler(CHANNEL_HANDLER_ID);
        super.onPause();
    }


    /**
     * Display which users are typing.
     * If more than two users are currently typing, this will state that "multiple users" are typing.
     *
     * @param typingUsers The list of currently typing users.
     */
    private void displayTyping(List<Member> typingUsers) {

        if (typingUsers.size() > 0) {
            mCurrentEventLayout.setVisibility(View.VISIBLE);
            String string;

            if (typingUsers.size() == 1) {
                string = String.format(getString(R.string.user_typing), typingUsers.get(0).getNickname());
            } else if (typingUsers.size() == 2) {
                string = String.format(getString(R.string.two_users_typing), typingUsers.get(0).getNickname(), typingUsers.get(1).getNickname());
            } else {
                string = getString(R.string.users_typing);
            }
            mCurrentEventText.setText(string);
        } else {
            mCurrentEventLayout.setVisibility(View.GONE);
        }
    }


    private void refresh() {
        if (mChannel == null) {
            GroupChannel.getChannel(mChannelUrl,
                    new GroupChannel.GroupChannelGetHandler() {
                        @Override
                        public void onResult(GroupChannel groupChannel, SendBirdException e) {
                            if (e != null) {
                                // Error!
                                e.printStackTrace();
                                return;
                            }

                            mChannel = groupChannel;
                            chatAdapter.setChannel(mChannel);
                            chatAdapter.loadLatestMessages(CHANNEL_LIST_LIMIT, new BaseChannel.GetMessagesHandler() {
                                @Override
                                public void onResult(List<BaseMessage> list, SendBirdException e) {
                                    chatAdapter.markAllMessagesAsRead();
                                }
                            });
//                    updateActionBarTitle();
                        }
                    });
        } else {
            mChannel.refresh(new GroupChannel.GroupChannelRefreshHandler() {
                @Override
                public void onResult(SendBirdException e) {
                    if (e != null) {
                        // Error!
                        e.printStackTrace();
                        return;
                    }

                    loadLatestMessages(CHANNEL_LIST_LIMIT, new BaseChannel.GetMessagesHandler() {
                        @Override
                        public void onResult(List<BaseMessage> list, SendBirdException e) {
                            chatAdapter.markAllMessagesAsRead();
                        }
                    });
//                    updateActionBarTitle();
                }
            });
        }
    }



    /**
     * Notify other users whether the current user is typing.
     *
     * @param typing Whether the user is currently typing.
     */
    private void setTypingStatus(boolean typing) {
        if (mChannel == null) {
            return;
        }

        if (typing) {
            mIsTyping = true;
            mChannel.startTyping();
        } else {
            mIsTyping = false;
            mChannel.endTyping();
        }
    }

}