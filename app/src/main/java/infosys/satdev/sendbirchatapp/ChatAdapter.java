package infosys.satdev.sendbirchatapp;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.sendbird.android.AdminMessage;
import com.sendbird.android.BaseChannel;
import com.sendbird.android.BaseMessage;
import com.sendbird.android.FileMessage;
import com.sendbird.android.GroupChannel;
import com.sendbird.android.SendBird;
import com.sendbird.android.SendBirdException;
import com.sendbird.android.User;
import com.sendbird.android.UserMessage;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Displays a list of Group Channels within a Sendbird application.
 */
public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

    private static final int VIEW_TYPE_USER_MESSAGE_ME = 10;
    private static final int VIEW_TYPE_USER_MESSAGE_OTHER = 11;
    private static final int VIEW_TYPE_FILE_MESSAGE_ME = 20;
    private static final int VIEW_TYPE_FILE_MESSAGE_OTHER = 21;
    private static final int VIEW_TYPE_FILE_MESSAGE_IMAGE_ME = 22;
    private static final int VIEW_TYPE_FILE_MESSAGE_IMAGE_OTHER = 23;
    private static final int VIEW_TYPE_FILE_MESSAGE_VIDEO_ME = 24;
    private static final int VIEW_TYPE_FILE_MESSAGE_VIDEO_OTHER = 25;
    private static final int VIEW_TYPE_ADMIN_MESSAGE = 30;

    private List<BaseMessage> mMessageList=new ArrayList<>();
    private Context mContext;


    private GroupChannel mChannel;
    ChatAdapter(Context context){
        mContext=context;
    }

    ChatAdapter(Context context,  List<BaseMessage> mMessageList){
        mContext = context;
        this.mMessageList=mMessageList;
//        Collections.reverse(mMessageList);

    }




    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        switch (viewType) {
            case VIEW_TYPE_USER_MESSAGE_ME:
                View myUserMsgView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.list_item_group_chat_user_me, parent, false);
                return new MyUserMessageHolder(myUserMsgView);
            case VIEW_TYPE_USER_MESSAGE_OTHER:
                View otherUserMsgView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.list_item_group_chat_user_other, parent, false);
                return new OtherUserMessageHolder(otherUserMsgView);
            default:
                return null;

        }


    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        BaseMessage message = mMessageList.get(position);
        boolean isContinuous = false;
        boolean isNewDay = false;
        boolean isTempMessage = false;
        Uri tempFileMessageUri = null;
        if (position < mMessageList.size() - 1) {
        // If the date of the previous message is different, display the date before the message,
        // and also set isContinuous to false to show information such as the sender's nickname
        // and profile image.
        if (!DateUtils.hasSameDate(message.getCreatedAt(), message.getCreatedAt())) {
            isNewDay = true;
            isContinuous = false;
        } else {
            isContinuous = isContinuous(message, message);
        }
    } else if ( position== mMessageList.size() - 1) {
        isNewDay = true;
    }

//    isTempMessage = isTempMessage(message);
//    tempFileMessageUri = getTempFileMessageUri(message);





        switch (holder.getItemViewType()) {
            case VIEW_TYPE_USER_MESSAGE_ME:
                ((MyUserMessageHolder) holder).bind(mContext, (UserMessage) message,mChannel,
                        isContinuous,isNewDay,
                        position);
                break;
            case VIEW_TYPE_USER_MESSAGE_OTHER:
                ((OtherUserMessageHolder) holder).bind(mContext, (UserMessage) message,
                                isContinuous,isNewDay,
                                position);
                break;

        }
    }

    @Override
    public int getItemCount() {
        return mMessageList.size();
    }

    /**
     * Declares the View Type according to the type of message and the sender.
     */
    @Override
    public int getItemViewType(int position) {
        BaseMessage message = mMessageList.get(position);

        if (message instanceof UserMessage) {
            UserMessage userMessage = (UserMessage) message;
            // If the sender is current user
            if (userMessage.getSender().getUserId()
                    .equals(SendBird.getCurrentUser().getUserId())) {
                return VIEW_TYPE_USER_MESSAGE_ME;
            } else {
                return VIEW_TYPE_USER_MESSAGE_OTHER;
            }
        }

        return -1;
    }


    /**
     * A ViewHolder that contains UI to display information about a GroupChannel.
     */

        private class MyUserMessageHolder extends RecyclerView.ViewHolder {
            TextView messageText, editedText, timeText, dateText;
            ViewGroup urlPreviewContainer;
            TextView urlPreviewSiteNameText, urlPreviewTitleText, urlPreviewDescriptionText;
            ImageView urlPreviewMainImageView;
            View padding;
            MessageStatusView messageStatusView;

            MyUserMessageHolder(View itemView) {
                super(itemView);

                messageText = (TextView) itemView.findViewById(R.id.text_group_chat_message);
                editedText = (TextView) itemView.findViewById(R.id.text_group_chat_edited);
                timeText = (TextView) itemView.findViewById(R.id.text_group_chat_time);
                dateText = (TextView) itemView.findViewById(R.id.text_group_chat_date);
                messageStatusView = itemView.findViewById(R.id.message_status_group_chat);

                urlPreviewContainer = (ViewGroup) itemView.findViewById(R.id.url_preview_container);
                urlPreviewSiteNameText = (TextView) itemView.findViewById(R.id.text_url_preview_site_name);
                urlPreviewTitleText = (TextView) itemView.findViewById(R.id.text_url_preview_title);
                urlPreviewDescriptionText = (TextView) itemView.findViewById(R.id.text_url_preview_description);
                urlPreviewMainImageView = (ImageView) itemView.findViewById(R.id.image_url_preview_main);

                // Dynamic padding that can be hidden or shown based on whether the message is continuous.
                padding = itemView.findViewById(R.id.view_group_chat_padding);
            }

            void bind(Context context, final UserMessage message,GroupChannel channel,
                      boolean isContinuous,boolean isNewDay,
                      final int position) {
                messageText.setText(message.getMessage());
                if (isNewDay) {
                    dateText.setVisibility(View.VISIBLE);
                    dateText.setText(DateUtils.formatDate(message.getCreatedAt()));
                } else {
                    dateText.setVisibility(View.GONE);
                }
                timeText.setText(DateUtils.formatTime(message.getCreatedAt()));

                if (message.getUpdatedAt() > 0) {
                    editedText.setVisibility(View.VISIBLE);
                } else {
                    editedText.setVisibility(View.GONE);
                }

                // If continuous from previous message, remove extra padding.
//                if (isContinuous) {
//                    padding.setVisibility(View.GONE);
//                } else {
//                    padding.setVisibility(View.VISIBLE);
//                }

                // If the message is sent on a different date than the previous one, display the date.
//                if (isNewDay) {
//                    dateText.setVisibility(View.VISIBLE);
////                    dateText.setText(DateUtils.formatDate(message.getCreatedAt()));
//                } else {
//                    dateText.setVisibility(View.GONE);
//                }

                urlPreviewContainer.setVisibility(View.GONE);
//                if (message.getCustomType().equals(URL_PREVIEW_CUSTOM_TYPE)) {
//                    try {
//                        urlPreviewContainer.setVisibility(View.VISIBLE);
//                        final UrlPreviewInfo  previewInfo = new UrlPreviewInfo(message.getData());
//                        urlPreviewSiteNameText.setText("@" + previewInfo.getSiteName());
//                        urlPreviewTitleText.setText(previewInfo.getTitle());
//                        urlPreviewDescriptionText.setText(previewInfo.getDescription());
////                        ImageUtils.displayImageFromUrl(context, previewInfo.getImageUrl(), urlPreviewMainImageView, null);
//                    } catch (JSONException e) {
//                        urlPreviewContainer.setVisibility(View.GONE);
//                        e.printStackTrace();
//                    }
//                }

//                if (clickListener != null) {
//                    itemView.setOnClickListener(new View.OnClickListener() {
//                        @Override
//                        public void onClick(View v) {
////                            clickListener.onUserMessageItemClick(message);
//                        }
//                    });
//                }
//
//                if (longClickListener != null) {
//                    itemView.setOnLongClickListener(new View.OnLongClickListener() {
//                        @Override
//                        public boolean onLongClick(View v) {
////                            longClickListener.onUserMessageItemLongClick(message, position);
//                            return true;
//                        }
//                    });
//                }

                messageStatusView.drawMessageStatus(channel, message);
            }
        }

        private class OtherUserMessageHolder extends RecyclerView.ViewHolder {
            TextView messageText, editedText, nicknameText, timeText, dateText;
            ImageView profileImage;

            ViewGroup urlPreviewContainer;
            TextView urlPreviewSiteNameText, urlPreviewTitleText, urlPreviewDescriptionText;
            ImageView urlPreviewMainImageView;

            public OtherUserMessageHolder(View itemView) {
                super(itemView);

                messageText = (TextView) itemView.findViewById(R.id.text_group_chat_message);
                editedText = (TextView) itemView.findViewById(R.id.text_group_chat_edited);
                timeText = (TextView) itemView.findViewById(R.id.text_group_chat_time);
                nicknameText = (TextView) itemView.findViewById(R.id.text_group_chat_nickname);
                profileImage = (ImageView) itemView.findViewById(R.id.image_group_chat_profile);
                dateText = (TextView) itemView.findViewById(R.id.text_group_chat_date);

                urlPreviewContainer = (ViewGroup) itemView.findViewById(R.id.url_preview_container);
                urlPreviewSiteNameText = (TextView) itemView.findViewById(R.id.text_url_preview_site_name);
                urlPreviewTitleText = (TextView) itemView.findViewById(R.id.text_url_preview_title);
                urlPreviewDescriptionText = (TextView) itemView.findViewById(R.id.text_url_preview_description);
                urlPreviewMainImageView = (ImageView) itemView.findViewById(R.id.image_url_preview_main);
            }


            void bind(Context context, final UserMessage message,boolean
                    isContinuous,boolean isNewDay,
                      final int position) {
//                 Show the date if the message was sent on a different date than the previous message.
                if (isNewDay) {
                    dateText.setVisibility(View.VISIBLE);
                    dateText.setText(DateUtils.formatDate(message.getCreatedAt()));
                } else {
                    dateText.setVisibility(View.GONE);
                }
                timeText.setText(DateUtils.formatTime(message.getCreatedAt()));

//                 Hide profile image and nickname if the previous message was also sent by current sender.
//                if (isContinuous) {
//                    profileImage.setVisibility(View.INVISIBLE);
//                    nicknameText.setVisibility(View.GONE);
//                } else {
//                    profileImage.setVisibility(View.VISIBLE);
////                    ImageUtils.displayRoundImageFromUrl(context, message.getSender().getProfileUrl(), profileImage);
//
//                    nicknameText.setVisibility(View.VISIBLE);
                    nicknameText.setText(message.getSender().getNickname());
//                }

                messageText.setText(message.getMessage());
//                timeText.setText(DateUtils.formatTime(message.getCreatedAt()));

//                if (message.getUpdatedAt() > 0) {
//                    editedText.setVisibility(View.VISIBLE);
//                } else {
//                    editedText.setVisibility(View.GONE);
//                }

//                urlPreviewContainer.setVisibility(View.GONE);
//                if (message.getCustomType().equals(URL_PREVIEW_CUSTOM_TYPE)) {
//                    try {
//                        urlPreviewContainer.setVisibility(View.VISIBLE);
//                        UrlPreviewInfo previewInfo = new UrlPreviewInfo(message.getData());
//                        urlPreviewSiteNameText.setText("@" + previewInfo.getSiteName());
//                        urlPreviewTitleText.setText(previewInfo.getTitle());
//                        urlPreviewDescriptionText.setText(previewInfo.getDescription());
//                        ImageUtils.displayImageFromUrl(context, previewInfo.getImageUrl(), urlPreviewMainImageView, null);
//                    } catch (JSONException e) {
//                        urlPreviewContainer.setVisibility(View.GONE);
//                        e.printStackTrace();
//                    }
//                }


//                if (clickListener != null) {
//                    itemView.setOnClickListener(new View.OnClickListener() {
//                        @Override
//                        public void onClick(View v) {
//                            clickListener.onUserMessageItemClick(message);
//                        }
//                    });
//                }
//                if (longClickListener != null) {
//                    itemView.setOnLongClickListener(new View.OnLongClickListener() {
//                        @Override
//                        public boolean onLongClick(View v) {
//                            longClickListener.onUserMessageItemLongClick(message, position);
//                            return true;
//                        }
//                    });
//                }
            }
        }




    void setChannel(GroupChannel channel) {
        mChannel = channel;
    }


    /**
     * Replaces current message list with new list.
     * Should be used only on initial load or refresh.
     */
    public void loadLatestMessages(int limit,
                                   final BaseChannel.GetMessagesHandler handler) {
        if (mChannel == null) {
            return;
        }

        if(isMessageListLoading()) {
            return;
        }

        setMessageListLoading(true);
        mChannel.getPreviousMessagesByTimestamp(Long.MAX_VALUE,
                true, limit, true, BaseChannel.MessageTypeFilter.ALL,
                null, new BaseChannel.GetMessagesHandler() {
            @Override
            public void onResult(List<BaseMessage> list, SendBirdException e) {
                if(handler != null) {
                    handler.onResult(list, e);
                }

                setMessageListLoading(false);
                if(e != null) {
                    e.printStackTrace();
                    return;
                }

                if(list.size() <= 0) {
                    return;
                }

                for (BaseMessage message : mMessageList) {
                    if (isTempMessage(message) || isFailedMessage(message)) {
                        list.add(0, message);
                    }
                }

                mMessageList.clear();

                for(BaseMessage message : list) {
                    mMessageList.add(message);
                }

                notifyDataSetChanged();
            }
        });
    }

    private boolean mIsMessageListLoading;

    private synchronized boolean isMessageListLoading() {
        return mIsMessageListLoading;
    }
    private synchronized void setMessageListLoading(boolean tf) {
        mIsMessageListLoading = tf;
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


    /**
     * Notifies that the user has read all (previously unread) messages in the channel.
     * Typically, this would be called immediately after the user enters the chat and loads
     * its most recent messages.
     */
    public void markAllMessagesAsRead() {
        if (mChannel != null) {
            mChannel.markAsRead();
        }
        notifyDataSetChanged();
    }


    public void markMessageFailed(BaseMessage message) {
        BaseMessage msg;
        for (int i = mMessageList.size() - 1; i >= 0; i--) {
            msg = mMessageList.get(i);
            if (msg.getRequestId().equals(message.getRequestId())) {
                mMessageList.set(i, message);
                notifyDataSetChanged();
                return;
            }
        }
    }


    /**
     * Load old message list.
     * @param limit
     * @param handler
     */
    public void loadPreviousMessages(int limit, final BaseChannel.GetMessagesHandler handler) {
        if (mChannel == null) {
            return;
        }

        if(isMessageListLoading()) {
            return;
        }

        long oldestMessageCreatedAt = Long.MAX_VALUE;
        if(mMessageList.size() > 0) {
            oldestMessageCreatedAt = mMessageList.get(mMessageList.size() - 1).getCreatedAt();
        }

        setMessageListLoading(true);
        mChannel.getPreviousMessagesByTimestamp(oldestMessageCreatedAt,
                false, limit, true, BaseChannel.MessageTypeFilter.ALL,
                null, new BaseChannel.GetMessagesHandler() {
                    @Override
                    public void onResult(List<BaseMessage> list, SendBirdException e) {
                        if(handler != null) {
                            handler.onResult(list, e);
                        }

                        setMessageListLoading(false);
                        if(e != null) {
                            e.printStackTrace();
                            return;
                        }

                        for(BaseMessage message : list) {
                            mMessageList.add(message);
                        }

                        notifyDataSetChanged();
                    }
                });
    }


    public void load(String channelUrl) {
        try {
            File appDir = new File(mContext.getCacheDir(), SendBird.getApplicationId());
            appDir.mkdirs();

            File dataFile = new File(appDir,
                    generateMD5(SendBird.getCurrentUser().getUserId() + channelUrl) + ".data");

            String content = loadFromFile(dataFile);
            String [] dataArray = content.split("\n");

            mChannel = (GroupChannel) GroupChannel.buildFromSerializedData(Base64.decode(dataArray[0], Base64.DEFAULT | Base64.NO_WRAP));

            // Reset message list, then add cached messages.
            mMessageList.clear();
            for(int i = 1; i < dataArray.length; i++) {
                mMessageList.add(BaseMessage.buildFromSerializedData(Base64.decode(dataArray[i], Base64.DEFAULT | Base64.NO_WRAP)));
            }

            notifyDataSetChanged();
        } catch(Exception e) {
            // Nothing to load.
        }
    }



    public void markMessageSent(BaseMessage message) {
        BaseMessage msg;
        for (int i = mMessageList.size() - 1; i >= 0; i--) {
            msg = mMessageList.get(i);
            if (message instanceof UserMessage && msg instanceof UserMessage) {
                if (((UserMessage) msg).getRequestId().equals(((UserMessage) message).getRequestId())) {
                    mMessageList.set(i, message);
                    notifyDataSetChanged();
                    return;
                }
            } else if (message instanceof FileMessage && msg instanceof FileMessage) {
                if (((FileMessage) msg).getRequestId().equals(((FileMessage) message).getRequestId())) {
//                    mTempFileMessageUriTable.remove(((FileMessage) message).getRequestId());
//                    mMessageList.set(i, message);
                    notifyDataSetChanged();
                    return;
                }
            }
        }
    }

    void addTempFileMessageInfo(FileMessage message, Uri uri) {
//        mTempFileMessageUriTable.put(message.getRequestId(), uri);
    }

    void addFirst(BaseMessage message) {
        mMessageList.add(0, message);
        notifyDataSetChanged();
    }

    void delete(long msgId) {
        for(BaseMessage msg : mMessageList) {
            if(msg.getMessageId() == msgId) {
                mMessageList.remove(msg);
                notifyDataSetChanged();
                break;
            }
        }
    }

    void update(BaseMessage message) {
        BaseMessage baseMessage;
        for (int index = 0; index < mMessageList.size(); index++) {
            baseMessage = mMessageList.get(index);
            if(message.getMessageId() == baseMessage.getMessageId()) {
                mMessageList.remove(index);
                mMessageList.add(index, message);
                notifyDataSetChanged();
                break;
            }
        }
    }




    // methods


    /**
     * Calculate MD5
     * @param data
     * @return
     * @throws NoSuchAlgorithmException
     */
    public static String generateMD5(String data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        digest.update(data.getBytes());
        byte messageDigest[] = digest.digest();

        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < messageDigest.length; i++)
            hexString.append(Integer.toHexString(0xFF & messageDigest[i]));

        return hexString.toString();
    }




    public static String loadFromFile(File file) throws IOException {
        FileInputStream stream = new FileInputStream(file);
        Reader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder builder = new StringBuilder();
        char[] buffer = new char[8192];
        int read;
        while ((read = reader.read(buffer, 0, buffer.length)) > 0) {
            builder.append(buffer, 0, read);
        }
        return builder.toString();
    }


    /**
     * Checks if the current message was sent by the same person that sent the preceding message.
     * <p>
     * This is done so that redundant UI, such as sender nickname and profile picture,
     * does not have to displayed when not necessary.
     */
    private boolean isContinuous(BaseMessage currentMsg, BaseMessage precedingMsg) {
        // null check
        if (currentMsg == null || precedingMsg == null) {
            return false;
        }

        if (currentMsg instanceof AdminMessage && precedingMsg instanceof AdminMessage) {
            return true;
        }

        User currentUser = null, precedingUser = null;

        if (currentMsg instanceof UserMessage) {
            currentUser = ((UserMessage) currentMsg).getSender();
        } else if (currentMsg instanceof FileMessage) {
            currentUser = ((FileMessage) currentMsg).getSender();
        }

        if (precedingMsg instanceof UserMessage) {
            precedingUser = ((UserMessage) precedingMsg).getSender();
        } else if (precedingMsg instanceof FileMessage) {
            precedingUser = ((FileMessage) precedingMsg).getSender();
        }

        // If admin message or
        return !(currentUser == null || precedingUser == null)
                && currentUser.getUserId().equals(precedingUser.getUserId());


    }

}
