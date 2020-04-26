package com.u16033361.ar.individualproject.samples.augmentedimage;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class BookAdapter extends RecyclerView.Adapter<BookAdapter.BookHolder> {
    private ArrayList<Book> books;
    private Context context;
    private onItemListener onItemListener;

    public BookAdapter(Context context, ArrayList<Book> books, onItemListener onItemListener) {
        this.context = context;
        this.books = books;
        this.onItemListener = onItemListener;
    }

    @NonNull
    @Override
    public BookHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
        View view = LayoutInflater.from(context).inflate(R.layout.recycler_view_template, parent, false);
        return new BookHolder(view, onItemListener);
    }

    @Override
    public void onBindViewHolder(@NonNull BookHolder bookHolder, int i) {
        Book book = books.get(i);
        bookHolder.setDetails(book, i);
    }

    @Override
    public int getItemCount() { return books.size(); }

    public class BookHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView imgCover;
        TextView txtNew;
        onItemListener onItemListener;

        public BookHolder(View itemView, onItemListener onItemListener) {
            super(itemView);
            imgCover = itemView.findViewById(R.id.imgCover);
            txtNew = itemView.findViewById(R.id.txtNewLabel);
            this.onItemListener = onItemListener;
            itemView.setOnClickListener(this);
        }

        //position never used but necessary.
        public void setDetails(Book book, int position) {
            imgCover.setImageBitmap(BitmapFactory.decodeByteArray(book.getImageByte(), 0, book.getImageByte().length));
            if(book.getNewEntry() == 1) txtNew.setVisibility(View.VISIBLE);
            else txtNew.setVisibility(View.INVISIBLE);
        }

        @Override
        public void onClick(View view) {
            onItemListener.onItemClick(getAdapterPosition());
        }
    }

    public interface onItemListener {
        void onItemClick(int position);
    }
}
