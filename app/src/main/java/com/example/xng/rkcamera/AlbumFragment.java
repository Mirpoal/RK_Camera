package com.example.xng.rkcamera;

/**
 * Created by Xng on 2016/8/4.
 */
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

/**
 * “我的”碎片页面
 * @author fbn
 *
 */
public class AlbumFragment extends Fragment {
    private Button photofilebtn,videofilebtn;
    private LinearLayout lin;
    private LinearLayout photolayout;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.album_fragment, container,
                false);
        photofilebtn =(Button)view.findViewById(R.id.photofile_btn);
        videofilebtn =(Button)view.findViewById(R.id.videofile_btn);
        lin = (LinearLayout) view.findViewById(R.id.album_layout);
        photolayout = (LinearLayout) inflater.inflate(
                R.layout.album_gridview,null).findViewById(R.id.album_grid_layout);

        photofilebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Intent intent = new Intent();
//                intent.setClass(getActivity(),AlbumGridFragment.class);
//                startActivity(intent);
                Activity activity = getActivity();
                if(activity instanceof MainActivity){
                    ((MainActivity) activity).clickTabLocalPhotoLayout();
                }
            }
        });
        videofilebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //点击事件
                Activity activity = getActivity();
                if(activity instanceof MainActivity){
                    ((MainActivity) activity).clickTabLocalVideoLayout();}
                }
        });
        return view;
    }

}
