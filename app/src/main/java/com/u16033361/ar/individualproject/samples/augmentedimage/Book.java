package com.u16033361.ar.individualproject.samples.augmentedimage;

import android.os.Parcel;
import android.os.Parcelable;
import android.widget.ImageView;

public class Book implements Parcelable {
    private byte[] imgCover;
    private String bookGenre, bookTitle, bookISBN, bookAuthor, bookDesc;
    private int bookReview, bookPages, bookDayAdded, newEntry, bookID;

    public Book(String bookGenre, String bookTitle, String bookISBN, String bookAuthor, String bookDesc,
                int bookReview, int bookPages, int bookDayAdded, byte[] imgCover, int newEntry, int bookID) {
        this.bookID = bookID;
        this.bookGenre = bookGenre;
        this.bookTitle = bookTitle;
        this.bookISBN = bookISBN;
        this.bookAuthor = bookAuthor;
        this.bookDesc = bookDesc;
        this.bookReview = bookReview;
        this.bookPages = bookPages;
        this.bookDayAdded = bookDayAdded;
        this.imgCover = imgCover;
        this.newEntry = newEntry;
    }

    //no-arg constructor
    public Book() { }

    public int getBookID() { return bookID; }
    public void setBookID(int bookID) { this.bookID = bookID; }

    public String getBookGenre() { return bookGenre; }
    public void setBookGenre(String bookGenre) { this.bookGenre = bookGenre; }

    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }

    public String getBookISBN() { return bookISBN; }
    public void setBookISBN(String bookISBN) { this.bookISBN = bookISBN; }

    public String getBookAuthor() { return bookAuthor; }
    public void setBookAuthor(String bookAuthor) { this.bookAuthor = bookAuthor; }

    public String getBookDesc() { return bookDesc; }
    public void setBookDesc(String bookDesc) { this.bookDesc = bookDesc; }

    public int getBookReview() { return bookReview; }
    public void setBookReview(int bookReview) { this.bookReview = bookReview; }

    public int getBookPages() { return bookPages; }
    public void setBookPages(int bookPages) { this.bookPages = bookPages; }

    public int getBookDayAdded() { return bookDayAdded; }
    public void setBookDayAdded(int bookDayAdded) { this.bookDayAdded = bookDayAdded; }

    public byte[] getImageByte() { return imgCover; }
    public void setImageByte(byte[] newImage) { this.imgCover = newImage; }

    public int getNewEntry() { return newEntry; }
    public void setNewEntry(int newEntry) { this.newEntry = newEntry; }

    @Override
    public int describeContents() { return 0; }

    protected Book(Parcel in) {
        bookID = in.readInt();
        bookGenre = in.readString();
        bookTitle = in.readString();
        bookISBN = in.readString();
        bookAuthor = in.readString();
        bookDesc = in.readString();
        bookReview = in.readInt();
        bookPages = in.readInt();
        bookDayAdded = in.readInt();
        newEntry = in.readInt();
        imgCover = new byte[in.readInt()];
        in.readByteArray(imgCover);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(bookID);
        dest.writeString(bookGenre);
        dest.writeString(bookTitle);
        dest.writeString(bookISBN);
        dest.writeString(bookAuthor);
        dest.writeString(bookDesc);
        dest.writeInt(bookReview);
        dest.writeInt(bookPages);
        dest.writeInt(bookDayAdded);
        dest.writeInt(newEntry);
        dest.writeInt(imgCover.length);
        dest.writeByteArray(imgCover);
    }

    public static final Creator<Book> CREATOR = new Creator<Book>() {
        @Override
        public Book createFromParcel(Parcel in) {
            return new Book(in);
        }
        @Override
        public Book[] newArray(int size) {
            return new Book[size];
        }
    };

}
