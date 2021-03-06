package com.linzhi.selectcamera_master.other;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.Toast;

import com.linzhi.selectcamera_master.R;
import com.linzhi.selectcamera_master.UserUploadPhotoClipPictureActivity;
import com.linzhi.selectcamera_master.other.ChooseItemDialog.ChooseItemDialogCallBack;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * 头像上传工具
 * 
 * @author Fangweidong
 * 
 */
public class UpdateAvatarUtil {

	public static final int REQUEST_CODE_PICK_PHOTO_FROM_CAMERA = 70;
	public static final int REQUEST_CODE_PICK_PHOTO_FROM_ALBUMS = 71;
	public static final int REQUEST_CODE_PICK_PHOTO_FINISH = 72;

	public static final int IMG_TYPE_AVATAR = -99;
	public static final int IMG_TYPE_USER_BG = -98;

	private Activity context;
	private Handler handler;
	private int updateType;
	private ChoosePicCallBack callBack;
	private String avatar = null;;
	private boolean uploadPic = false;
	//构造赋值
	public UpdateAvatarUtil(Activity context, Handler handler,ChoosePicCallBack callBack, boolean uploadPic) {
		this.context = context;
		this.handler = handler;
		this.callBack = callBack;
		this.uploadPic = uploadPic;
	}
	//接口方法
	public interface ChoosePicCallBack {
		//在CreateUserActivty中实现
		void updateAvatarSuccess(int updateType, String avatar, String avatarBase64);
		//在CreateUserActivty中实现
		void updateAvatarFailed(int updateType);

		void cancel();
	}

	// CreateUserActivity--UpdateAvatar该方法：选取照片对话框
	public void showChoosePhotoDialog(int updateType) {

		this.updateType = updateType;
		//实例化ChooseItemDialog
		ChooseItemDialog dialog = new ChooseItemDialog(context, 
				"拍照", 
				"从相册中选择",
				0,
				false,
				new ChooseItemDialogCallBack() {
					//实现父类ChooseItemDialog.ChooseItemDialogCallBack的两个接口方法（1）
					@Override
					public void confirm(int whichItem) {
						if (whichItem == 0) {
							// 拍照
							//图片路径:VFACE(三种情况)/tempPics/uploadTemp/unhandled.jpg
							String path = MyApplication.getUnhandledUserPhotoPath(context);
							File newfile = new File(path);// file
							try {
								newfile.createNewFile();//创建
							} catch (IOException e) {
								e.printStackTrace();
							}
							Uri outputFileUri = Uri.fromFile(newfile);
							Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
							cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
							context.startActivityForResult(cameraIntent,
									REQUEST_CODE_PICK_PHOTO_FROM_CAMERA);//70
						} else if(whichItem == 1) {
							// 从相册中选择
							Intent pickPhoto = new Intent(Intent.ACTION_PICK, null);
							//设置可以浏览的图片类型：.jpeg/.jpg
							pickPhoto.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
							context.startActivityForResult(pickPhoto,
									REQUEST_CODE_PICK_PHOTO_FROM_ALBUMS);//71
						}
					}
					//实现父类ChooseItemDialog.ChooseItemDialogCallBack的两个接口方法（2）
					@Override
					public void cancel() {
						if (callBack != null) {
							callBack.cancel();
						}
					}
				});
		dialog.show();

	}

	/**
	 * 在onActivityResult里调用
	 * 
	 * @param requestCode
	 * @param resultCode
	 * @param data
	 */
	public void onActivityResultAction(int requestCode, int resultCode,
			Intent data) {
		if (requestCode == REQUEST_CODE_PICK_PHOTO_FROM_ALBUMS) {
			if (resultCode == Activity.RESULT_OK) {
				Uri uri = data.getData();

				String[] proj = { MediaStore.Images.Media.DATA };
				Cursor cursor = context.managedQuery(uri, proj, null, null,
						null);
				if (cursor != null) {
					try {
						int actual_image_column_index = cursor
								.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
						cursor.moveToFirst();
						String img_path = cursor
								.getString(actual_image_column_index);

						Intent i = new Intent();
						i.putExtra("img_path", img_path);
						i.setClass(context,
								UserUploadPhotoClipPictureActivity.class);

						context.startActivityForResult(i,
								REQUEST_CODE_PICK_PHOTO_FINISH);

					} catch (Exception e) {
						Toast.makeText(
								context,
								context.getString(R.string.please_choose_system_album_pic),
								Toast.LENGTH_SHORT).show();
					}
				} else {
					Toast.makeText(
							context,
							context.getString(R.string.please_choose_system_album_pic),
							Toast.LENGTH_SHORT).show();
				}

			}
		} else if (requestCode == REQUEST_CODE_PICK_PHOTO_FROM_CAMERA) {
			if (resultCode == Activity.RESULT_OK) {
				Intent i = new Intent();
				i.setClass(context, UserUploadPhotoClipPictureActivity.class);
				context.startActivityForResult(i,
						REQUEST_CODE_PICK_PHOTO_FINISH);
			}
		} else if (requestCode == REQUEST_CODE_PICK_PHOTO_FINISH) {
			if (resultCode == Activity.RESULT_OK) {
				if (uploadPic) {
					// 裁减成功
					updateAvatar();
				} else { 
					String picPath = MyApplication.getHandledUserPhotoPath(context);
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					File picFile = new File(picPath);
					String avatarBase64 ="";
					if (picFile.exists()) {
						try {
							FileInputStream fis = new FileInputStream(picFile);
							byte[] buffer = new byte[128];
							int iLength = 0;
							while ((iLength = fis.read(buffer)) != -1) {
								baos.write(buffer, 0, iLength);
							}
							avatarBase64 = new String(Base64.encode(
									baos.toByteArray(), Base64.DEFAULT));
							fis.close();
							baos.close(); 
						}
						catch(Exception ex){
							ex.printStackTrace();
						}
					}
					callBack.updateAvatarSuccess(updateType, picPath, avatarBase64);
				}

			}
		}
	}

	/**
	 * 头像图片转base64上传,成功 失败都回调到具体act中执行
	 */
	public void updateAvatar() {
		Loading.run(context, new Runnable() {
			@Override
			public void run() {
				String picPath = MyApplication.getHandledUserPhotoPath(context);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				File picFile = new File(picPath);
				if (picFile.exists()) {
					try {
						FileInputStream fis = new FileInputStream(picFile);
						byte[] buffer = new byte[128];
						int iLength = 0;
						while ((iLength = fis.read(buffer)) != -1) {
							baos.write(buffer, 0, iLength);
						}
						final String avatarBase64 = new String(Base64.encode(
								baos.toByteArray(), Base64.DEFAULT));
						fis.close();
						baos.close();
						if (updateType == IMG_TYPE_AVATAR) {
							// avatar = ProfileHelper.updateAvatar(context,
							// avatarBase64);
						} else if (updateType == IMG_TYPE_USER_BG) {
							// avatar = ProfileHelper.updateUserBg(context,
							// avatarBase64);
						}
						handler.post(new Runnable() {

							@Override
							public void run() {
								callBack.updateAvatarSuccess(updateType,
										avatar, avatarBase64);
							}
						});
						return;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				handler.post(new Runnable() {

					@Override
					public void run() {
						callBack.updateAvatarFailed(updateType);
					}
				});
			}
		});
	}

}
