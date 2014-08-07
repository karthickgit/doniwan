/**
 * Copyright (C) 2013-2014 EaseMob Technologies. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.easemob.chatuidemo.activity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.easemob.EMCallBack;
import com.easemob.chat.EMChatManager;
import com.easemob.chat.EMContact;
import com.easemob.chat.EMConversation;
import com.easemob.chat.EMGroup;
import com.easemob.chat.EMGroupManager;
import com.easemob.chat.EMMessage;
import com.easemob.chat.ImageMessageBody;
import com.easemob.chat.VoiceMessageBody;
import com.easemob.chatuidemo.DemoApplication;
import com.easemob.chatuidemo.R;
import com.easemob.chatuidemo.adapter.ChatHistoryAdapter;
import com.easemob.chatuidemo.adapter.VoicePlayClickListener;
import com.easemob.chatuidemo.db.InviteMessgeDao;
import com.easemob.chatuidemo.domain.User;
import com.easemob.chatuidemo.image.FileImageDecoder;
import com.easemob.chatuidemo.image.ImageDecoder.ImageScaleType;
import com.easemob.chatuidemo.image.ImageSize;
import com.easemob.chatuidemo.utils.CommonUtils;
import com.easemob.chatuidemo.utils.ImageUtils;
import com.easemob.util.VoiceRecorder;

/**
 * 聊天记录Fragment
 * 
 */
public class ChatHistoryFragment extends Fragment {

	private InputMethodManager inputMethodManager;
	private ListView listView;
	private Map<String, User> contactList;
	private ChatHistoryAdapter adapter;
	private EditText query;
	private ImageButton clearSearch;
	public RelativeLayout errorItem;
	public TextView errorText;
	private boolean hidden;
	private TextView unread_msg_number;
	private ImageView face;
	private VoiceRecorder voiceRecorder;
	private View buttonPressToSpeak;
	
	private PowerManager.WakeLock wakeLock;
	View recordingContainer;
	ImageView micImage;
	private Drawable[] micImages;
	TextView recordingHint;
	
	String image = "/storage/emulated/legacy/Pictures/ps.png";

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_conversation_history, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
		errorItem = (RelativeLayout) getView().findViewById(R.id.rl_error_item);
		errorText = (TextView) errorItem.findViewById(R.id.tv_connect_errormsg);
		// contact list
		contactList = DemoApplication.getInstance().getContactList();
		listView = (ListView) getView().findViewById(R.id.list);
		adapter = new ChatHistoryAdapter(getActivity(), 1, loadUsersWithRecentChat());
		unread_msg_number = (TextView)getView().findViewById(R.id.unread_msg_number);
		unread_msg_number.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				openFace();
			}
		});
		face = (ImageView)getView().findViewById(R.id.face);
		recordingContainer = getView().findViewById(R.id.recording_container);
		micImage = (ImageView) getView().findViewById(R.id.mic_image);
		// 动画资源文件,用于录制语音时
		micImages = new Drawable[] { getResources().getDrawable(R.drawable.record_animate_01),
				getResources().getDrawable(R.drawable.record_animate_02), getResources().getDrawable(R.drawable.record_animate_03),
				getResources().getDrawable(R.drawable.record_animate_04), getResources().getDrawable(R.drawable.record_animate_05),
				getResources().getDrawable(R.drawable.record_animate_06), getResources().getDrawable(R.drawable.record_animate_07),
				getResources().getDrawable(R.drawable.record_animate_08), getResources().getDrawable(R.drawable.record_animate_09),
				getResources().getDrawable(R.drawable.record_animate_10), getResources().getDrawable(R.drawable.record_animate_11),
				getResources().getDrawable(R.drawable.record_animate_12), getResources().getDrawable(R.drawable.record_animate_13),
				getResources().getDrawable(R.drawable.record_animate_14), };
				
		recordingHint = (TextView) getView().findViewById(R.id.recording_hint);
		buttonPressToSpeak = getView().findViewById(R.id.btn_press_to_speak);
		buttonPressToSpeak.setOnTouchListener(new PressToSpeakListen());
		wakeLock = ((PowerManager) getActivity().getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "demo");
		
		FileImageDecoder decoder = new FileImageDecoder(new File(image));
		Bitmap bitmap;
		try {
			bitmap = decoder.decode(new ImageSize(300, 300), ImageScaleType.POWER_OF_2);
			face.setImageBitmap(bitmap);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		voiceRecorder = new VoiceRecorder(new Handler() {
			@Override
			public void handleMessage(android.os.Message msg) {
				micImage.setImageDrawable(micImages[msg.what]);
			}
		});
		
		// 设置adapter
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				EMContact emContact = adapter.getItem(position);
				if (adapter.getItem(position).getUsername().equals(DemoApplication.getInstance().getUserName()))
					Toast.makeText(getActivity(), "不能和自己聊天", 0).show();
				else {
//					// 进入聊天页面
//					  Intent intent = new Intent(getActivity(), ChatActivity.class);
//					 if (emContact instanceof EMGroup) {
//		                    //it is group chat
//		                    intent.putExtra("chatType", ChatActivity.CHATTYPE_GROUP);
//		                    intent.putExtra("groupId", ((EMGroup) emContact).getGroupId());
//		                } else {
//		                    //it is single chat
//		                    intent.putExtra("userId", emContact.getUsername());
//		                } 
//					startActivity(intent);
					
					//TODOHAND 发送消息
					String username = emContact.getUsername();
					sendFace(image, username);
					
				}
			}
		});
		// 注册上下文菜单
		registerForContextMenu(listView);

		listView.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// 隐藏软键盘
				if (getActivity().getWindow().getAttributes().softInputMode != WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN) {
					if (getActivity().getCurrentFocus() != null)
						inputMethodManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(),
								InputMethodManager.HIDE_NOT_ALWAYS);
				}
				return false;
			}
		});
		// 搜索框
		query = (EditText) getView().findViewById(R.id.query);
		// 搜索框中清除button
		clearSearch = (ImageButton) getView().findViewById(R.id.search_clear);
		query.addTextChangedListener(new TextWatcher() {
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				adapter.getFilter().filter(s);
				if (s.length() > 0) {
					clearSearch.setVisibility(View.VISIBLE);
				} else {
					clearSearch.setVisibility(View.INVISIBLE);
				}
			}

			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			public void afterTextChanged(Editable s) {
			}
		});
		clearSearch.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				query.getText().clear();

			}
		});

	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		// if(((AdapterContextMenuInfo)menuInfo).position > 0){ m,
		getActivity().getMenuInflater().inflate(R.menu.delete_message, menu);
		// }
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.delete_message) {
			EMContact tobeDeleteUser = adapter.getItem(((AdapterContextMenuInfo) item.getMenuInfo()).position);
			// 删除此会话
			EMChatManager.getInstance().deleteConversation(tobeDeleteUser.getUsername());
			InviteMessgeDao inviteMessgeDao = new InviteMessgeDao(getActivity());
			inviteMessgeDao.deleteMessage(tobeDeleteUser.getUsername());
			adapter.remove(tobeDeleteUser);
			adapter.notifyDataSetChanged();

			// 更新消息未读数
			((MainActivity) getActivity()).updateUnreadLabel();

			return true;
		}
		return super.onContextItemSelected(item);
	}
	
	/**
	 * 按住说话listener
	 * 
	 */
	class PressToSpeakListen implements View.OnTouchListener {
		EMContact user = loadUsersWithRecentChat().get(0);
		String toChatUsername =  user.getUsername();
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				if (!CommonUtils.isExitsSdcard()) {
					Toast.makeText(getActivity(), "发送语音需要sdcard支持！", Toast.LENGTH_SHORT).show();
					return false;
				}
				try {
					v.setPressed(true);
					wakeLock.acquire();
					if (VoicePlayClickListener.isPlaying)
						VoicePlayClickListener.currentPlayListener.stopPlayVoice();
					recordingContainer.setVisibility(View.VISIBLE);
					recordingHint.setText(getString(R.string.move_up_to_cancel));
					recordingHint.setBackgroundColor(Color.TRANSPARENT);
					voiceRecorder.startRecording(null, toChatUsername, getActivity().getApplicationContext());
				} catch (Exception e) {
					e.printStackTrace();
					v.setPressed(false);
					if (wakeLock.isHeld())
						wakeLock.release();
					recordingContainer.setVisibility(View.INVISIBLE);
					Toast.makeText(getActivity(), R.string.recoding_fail, Toast.LENGTH_SHORT).show();
					return false;
				}

				return true;
			case MotionEvent.ACTION_MOVE: {
				if (event.getY() < 0) {
					recordingHint.setText(getString(R.string.release_to_cancel));
					recordingHint.setBackgroundResource(R.drawable.recording_text_hint_bg);
				} else {
					recordingHint.setText(getString(R.string.move_up_to_cancel));
					recordingHint.setBackgroundColor(Color.TRANSPARENT);
				}
				return true;
			}
			case MotionEvent.ACTION_UP:
				v.setPressed(false);
				recordingContainer.setVisibility(View.INVISIBLE);
				if (wakeLock.isHeld())
					wakeLock.release();
				if (event.getY() < 0) {
					// discard the recorded audio.
					voiceRecorder.discardRecording();
				} else {
					// stop recording and send voice file
					try {
						int length = voiceRecorder.stopRecoding();
						if (length > 0) {
							sendVoice(toChatUsername, voiceRecorder.getVoiceFilePath(), voiceRecorder.getVoiceFileName(toChatUsername), Integer.toString(length), false);
						} else {
							Toast.makeText(getActivity(), "录音时间太短", 0).show();
						}
					} catch (Exception e) {
						e.printStackTrace();
						Toast.makeText(getActivity(), "发送失败，请检测服务器是否连接", Toast.LENGTH_SHORT).show();
					}

				}
				return true;
			default:
				return false;
			}
		}
	}
	
	/**
	 * TODOHAND 发送语音
	 */
	private void sendVoice(String to, String filePath, String fileName, String length, boolean isResend) {
		if (!(new File(filePath).exists())) {
			return;
		}
		try {
			final EMMessage message = EMMessage.createSendMessage(EMMessage.Type.VOICE);
			message.setReceipt(to);
			int len = Integer.parseInt(length);
			VoiceMessageBody body = new VoiceMessageBody(new File(filePath), len);
			message.addBody(body);

			EMConversation conversation = EMChatManager.getInstance().getConversation(to);
			conversation.addMessage(message);
			EMChatManager.getInstance().sendMessage(message, new EMCallBack() {
				@Override
				public void onSuccess() {
				}
				
				@Override
				public void onProgress(int arg0, String arg1) {
				}
				
				@Override
				public void onError(int arg0, String arg1) {
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 接收到消息后回调
	 */
	private void onReveicedMessages(){
		List<EMContact> chats = loadUsersWithRecentChat();
		int total = 0;
		for(EMContact user : chats){
			String username = user.getUsername();
			EMConversation conversation = EMChatManager.getInstance().getConversation(username);
			int count = conversation.getUnreadMsgCount();
			total += count;
		}
		System.out.println(String.format(" total: %s ", total));
		
		if(total > 0){
			unread_msg_number.setVisibility(View.VISIBLE);
			unread_msg_number.setText("" + total);
		}else{
			unread_msg_number.setVisibility(View.GONE);
		}
	}
	
	/**
	 * 发送表情
	 */
	private void sendFace(final String filePath, String toChatUsername) {
		String to = toChatUsername;
		// create and add image message in view
		final EMMessage message = EMMessage.createSendMessage(EMMessage.Type.IMAGE);
		message.setReceipt(to);
		ImageMessageBody body = new ImageMessageBody(new File(filePath));
		// 默认超过100k的图片会压缩后发给对方，可以设置成发送原图
		// body.setSendOriginalImage(true);
		message.addBody(body);
		
		EMConversation conversation = EMChatManager.getInstance().getConversation(toChatUsername);
		conversation.addMessage(message);
		EMChatManager.getInstance().sendMessage(message, new EMCallBack() {
			@Override
			public void onSuccess() {
			}
			
			@Override
			public void onProgress(int arg0, String arg1) {
			}
			
			@Override
			public void onError(int arg0, String arg1) {
			}
		});
	}
	
	/**
	 * 打开表情
	 */
	public void openFace(){
		EMContact user = loadUsersWithRecentChat().get(0);
		if(user != null){
			String username = user.getUsername();
			EMConversation conversation = EMChatManager.getInstance().getConversation(username);
			EMMessage message = conversation.getMessage(conversation.getMsgCount() -1);
			EMMessage voiceMessage = conversation.getMessage(conversation.getMsgCount() -2);
			
			if(!com.easemob.chat.EMMessage.Type.IMAGE.equals(message.getType())) return;
			
			ImageMessageBody imgBody = (ImageMessageBody) message.getBody();
			if (imgBody.getLocalUrl() != null) {
				String filePath = imgBody.getLocalUrl();

				String thumbnailPath = ImageUtils.getThumbnailImagePath(filePath);
				Intent intent = new Intent(getActivity(), ShowBigImage.class);
				File file = new File(thumbnailPath);
				if (file.exists()) {
					Uri uri = Uri.fromFile(file);
					intent.putExtra("uri", uri);
					System.err.println("here need to check why download everytime");
				} else {
					String remoteDir = imgBody.getRemoteUrl();
					ImageMessageBody body = (ImageMessageBody) message.getBody();
					intent.putExtra("secret", body.getSecret());
					intent.putExtra("remotepath", remoteDir);
				}
				
				ShowBigImage.putValus("voice", voiceMessage);
				startActivity(intent);
			}
		}
	}

	/**
	 * 刷新页面
	 */
	public void refresh() {
		adapter = new ChatHistoryAdapter(getActivity(), R.layout.row_chat_history, loadUsersWithRecentChat());
		listView.setAdapter(adapter);
		adapter.notifyDataSetChanged();
		onReveicedMessages();
	}

	/**
	 * 获取有聊天记录的users和groups
	 * 
	 * @param context
	 * @return
	 */
	private List<EMContact> loadUsersWithRecentChat() {
		List<EMContact> resultList = new ArrayList<EMContact>();
		for (User user : contactList.values()) {
			EMConversation conversation = EMChatManager.getInstance().getConversation(user.getUsername());
			if (conversation.getMsgCount() > 0) {
				resultList.add(user);
			}
		}
		for(EMGroup group : EMGroupManager.getInstance().getAllGroups()){
			EMConversation conversation = EMChatManager.getInstance().getConversation(group.getGroupId());
			if(conversation.getMsgCount() > 0){
				resultList.add(group);
			}
			
		}
		// 排序
		sortUserByLastChatTime(resultList);
		return resultList;
	}

	/**
	 * 根据最后一条消息的时间排序
	 * 
	 * @param usernames
	 */
	private void sortUserByLastChatTime(List<EMContact> contactList) {
		Collections.sort(contactList, new Comparator<EMContact>() {
			@Override
			public int compare(final EMContact user1, final EMContact user2) {
				EMConversation conversation1 = EMChatManager.getInstance().getConversation(user1.getUsername());
				EMConversation conversation2 = EMChatManager.getInstance().getConversation(user2.getUsername());

				EMMessage user2LastMessage = conversation2.getLastMessage();
				EMMessage user1LastMessage = conversation1.getLastMessage();
				if (user2LastMessage.getMsgTime() == user1LastMessage.getMsgTime()) {
					return 0;
				} else if (user2LastMessage.getMsgTime() > user1LastMessage.getMsgTime()) {
					return 1;
				} else {
					return -1;
				}
			}

		});
	}

	@Override
	public void onHiddenChanged(boolean hidden) {
		super.onHiddenChanged(hidden);
		this.hidden = hidden;
		if (!hidden) {
			refresh();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (!hidden) {
			refresh();
		}
	}
	
	
}
