package com.squareup.okhttp.internal.framed;

import java.util.Arrays;

public final class Settings {
   static final int DEFAULT_INITIAL_WINDOW_SIZE = 65536;
   static final int FLAG_CLEAR_PREVIOUSLY_PERSISTED_SETTINGS = 1;
   static final int PERSIST_VALUE = 1;
   static final int PERSISTED = 2;
   static final int UPLOAD_BANDWIDTH = 1;
   static final int HEADER_TABLE_SIZE = 1;
   static final int DOWNLOAD_BANDWIDTH = 2;
   static final int ENABLE_PUSH = 2;
   static final int ROUND_TRIP_TIME = 3;
   static final int MAX_CONCURRENT_STREAMS = 4;
   static final int CURRENT_CWND = 5;
   static final int MAX_FRAME_SIZE = 5;
   static final int DOWNLOAD_RETRANS_RATE = 6;
   static final int MAX_HEADER_LIST_SIZE = 6;
   static final int INITIAL_WINDOW_SIZE = 7;
   static final int CLIENT_CERTIFICATE_VECTOR_SIZE = 8;
   static final int FLOW_CONTROL_OPTIONS = 10;
   static final int COUNT = 10;
   static final int FLOW_CONTROL_OPTIONS_DISABLED = 1;
   private int set;
   private int persistValue;
   private int persisted;
   private final int[] values = new int[10];

   void clear() {
      this.set = this.persistValue = this.persisted = 0;
      Arrays.fill(this.values, 0);
   }

   Settings set(int id, int idFlags, int value) {
      if (id >= this.values.length) {
         return this;
      } else {
         int bit = 1 << id;
         this.set |= bit;
         if ((idFlags & 1) != 0) {
            this.persistValue |= bit;
         } else {
            this.persistValue &= ~bit;
         }

         if ((idFlags & 2) != 0) {
            this.persisted |= bit;
         } else {
            this.persisted &= ~bit;
         }

         this.values[id] = value;
         return this;
      }
   }

   boolean isSet(int id) {
      int bit = 1 << id;
      return (this.set & bit) != 0;
   }

   int get(int id) {
      return this.values[id];
   }

   int flags(int id) {
      int result = 0;
      if (this.isPersisted(id)) {
         result |= 2;
      }

      if (this.persistValue(id)) {
         result |= 1;
      }

      return result;
   }

   int size() {
      return Integer.bitCount(this.set);
   }

   int getUploadBandwidth(int defaultValue) {
      int bit = 2;
      return (bit & this.set) != 0 ? this.values[1] : defaultValue;
   }

   int getHeaderTableSize() {
      int bit = 2;
      return (bit & this.set) != 0 ? this.values[1] : -1;
   }

   int getDownloadBandwidth(int defaultValue) {
      int bit = 4;
      return (bit & this.set) != 0 ? this.values[2] : defaultValue;
   }

   boolean getEnablePush(boolean defaultValue) {
      int bit = 4;
      return ((bit & this.set) != 0 ? this.values[2] : (defaultValue ? 1 : 0)) == 1;
   }

   int getRoundTripTime(int defaultValue) {
      int bit = 8;
      return (bit & this.set) != 0 ? this.values[3] : defaultValue;
   }

   int getMaxConcurrentStreams(int defaultValue) {
      int bit = 16;
      return (bit & this.set) != 0 ? this.values[4] : defaultValue;
   }

   int getCurrentCwnd(int defaultValue) {
      int bit = 32;
      return (bit & this.set) != 0 ? this.values[5] : defaultValue;
   }

   int getMaxFrameSize(int defaultValue) {
      int bit = 32;
      return (bit & this.set) != 0 ? this.values[5] : defaultValue;
   }

   int getDownloadRetransRate(int defaultValue) {
      int bit = 64;
      return (bit & this.set) != 0 ? this.values[6] : defaultValue;
   }

   int getMaxHeaderListSize(int defaultValue) {
      int bit = 64;
      return (bit & this.set) != 0 ? this.values[6] : defaultValue;
   }

   int getInitialWindowSize(int defaultValue) {
      int bit = 128;
      return (bit & this.set) != 0 ? this.values[7] : defaultValue;
   }

   int getClientCertificateVectorSize(int defaultValue) {
      int bit = 256;
      return (bit & this.set) != 0 ? this.values[8] : defaultValue;
   }

   boolean isFlowControlDisabled() {
      int bit = 1024;
      int value = (bit & this.set) != 0 ? this.values[10] : 0;
      return (value & 1) != 0;
   }

   boolean persistValue(int id) {
      int bit = 1 << id;
      return (this.persistValue & bit) != 0;
   }

   boolean isPersisted(int id) {
      int bit = 1 << id;
      return (this.persisted & bit) != 0;
   }

   void merge(Settings other) {
      for(int i = 0; i < 10; ++i) {
         if (other.isSet(i)) {
            this.set(i, other.flags(i), other.get(i));
         }
      }

   }
}
