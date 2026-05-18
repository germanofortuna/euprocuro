export type User = {
  id?: string;
  name?: string;
  email?: string;
  postalCode?: string;
  city?: string;
  state?: string;
  neighborhood?: string;
  country?: string;
  credits?: number;
  sellerCredits?: number;
  admin?: boolean;
  isAdmin?: boolean;
  role?: string;
  roles?: string[];
  authorities?: string[];
};

export type StoredSession = {
  expiresAt: string | null;
  token: string | null;
  user: User | null;
};

export type Category = {
  code?: string;
  value: string;
  label: string;
  active?: boolean;
  sortOrder?: number;
};

export type LocationInfo = {
  postalCode?: string | null;
  city?: string | null;
  state?: string | null;
  neighborhood?: string | null;
  country?: string | null;
  remote?: boolean;
};

export type InterestStatus =
  | "PENDING"
  | "OPEN"
  | "APPROVED"
  | "REVIEW_REQUIRED"
  | "REJECTED"
  | "REPORTED"
  | "HIDDEN"
  | "CLOSED";

export type Interest = {
  id: string;
  ownerId?: string | null;
  ownerName?: string | null;
  title: string;
  description: string;
  referenceImageUrl?: string | null;
  category: string;
  budgetMin?: number | null;
  budgetMax?: number | null;
  location?: LocationInfo | null;
  tags?: string[];
  stickerDetails?: {
    type?: "MISSING" | "AVAILABLE" | string;
    group?: string | null;
    selection?: string | null;
    numbers?: string[];
    players?: string[];
  } | null;
  desiredRadiusKm?: number | null;
  allowsWhatsappContact?: boolean;
  whatsappContact?: string | null;
  boostedUntil?: string | null;
  preferredCondition?: string | null;
  preferredContactMode?: string | null;
  status?: InterestStatus | null;
  moderation?: unknown;
  createdAt?: string;
  updatedAt?: string;
  expiresAt?: string | null;
};

export type Offer = {
  id: string;
  interestId?: string;
  interestPostId?: string;
  interestTitle?: string;
  referenceImageUrl?: string | null;
  sellerId?: string;
  buyerId?: string;
  sellerName?: string;
  sellerEmail?: string;
  sellerPhone?: string;
  buyerName?: string;
  offeredPrice?: number | null;
  message?: string;
  highlights?: string[];
  status?: string;
  createdAt?: string;
  latestMessageAt?: string;
  latestMessageSenderId?: string;
  latestMessage?: string;
  offerImageUrl?: string | null;
  includesDelivery?: boolean;
};

export type ConversationMessage = {
  id: string;
  senderId?: string;
  senderName?: string;
  content: string;
  imageUrl?: string | null;
  createdAt?: string;
};

export type OfferConversation = Offer & {
  offerId?: string;
  messages?: ConversationMessage[];
};

export type SellerItem = {
  id?: string;
  title?: string;
  name?: string;
  description?: string;
  category?: string;
  desiredPrice?: number | null;
  referenceImageUrl?: string | null;
  active?: boolean;
  location?: LocationInfo | null;
  tags?: string[];
};

export type SellerItemGroup = {
  item?: SellerItem;
  matchingInterests?: Interest[];
  matchCount?: number;
};

export type MonetizationProduct = {
  code: string;
  name: string;
  description?: string;
  type: "CREDIT_PACK" | "SUBSCRIPTION" | "BOOST" | string;
  price?: number;
  originalPrice?: number | null;
  promotional?: boolean;
  promotionLabel?: string | null;
  credits?: number | null;
  durationDays?: number | null;
  enabled?: boolean;
  sortOrder?: number;
};

export type MonetizationAccount = {
  sellerCredits?: number;
  purchasedCreditsTotal?: number;
  subscriptionPlan?: string | null;
  subscriptionActiveUntil?: string | null;
  subscriptionActive?: boolean;
  proSubscriptionActive?: boolean;
  creditPurchasesEnabled?: boolean;
  boostPurchasesEnabled?: boolean;
  products?: MonetizationProduct[];
  paymentHistory?: Array<{
    id?: string;
    productName?: string;
    productCode?: string;
    status?: string;
    paymentMethod?: string;
    amount?: number;
    createdAt?: string;
  }>;
  payments?: Array<{
    id?: string;
    productName?: string;
    productCode?: string;
    status?: string;
    paymentMethod?: string;
    amount?: number;
    createdAt?: string;
  }>;
  settings?: {
    creditPurchasesEnabled?: boolean;
    boostPurchasesEnabled?: boolean;
  };
};

export type OperationalSettings = {
  featureFlags?: {
    stickersPageEnabled?: boolean;
  };
  operationalFields?: {
    initialFreeCredits?: number;
  };
};

export type Dashboard = {
  user?: User;
  totalActiveInterests?: number;
  totalOffersSent?: number;
  totalOffersReceived?: number;
  myInterests?: Interest[];
  sentOffers?: Offer[];
  receivedOffers?: Offer[];
  offersSent?: Offer[];
  offersReceived?: Offer[];
};

export type PublicContentEntry = {
  key: string;
  type?: string;
  locale?: string;
  version?: number;
  value: string;
  legalSlug?: string | null;
  requiresUserAcceptance?: boolean;
  publishedAt?: string;
};

export type PublicContentCatalog = {
  locale?: string;
  version?: string;
  entries?: Record<string, PublicContentEntry | string>;
};

export type AdminModeration = {
  pendingInterests?: Interest[];
  rules?: Array<Record<string, unknown>>;
  openReports?: Array<Record<string, unknown>>;
  processedReports?: Array<Record<string, unknown>>;
};

export type OmbudsmanRequest = {
  id?: string;
  protocol?: string;
  name?: string;
  email?: string;
  type?: string;
  subject?: string;
  message?: string;
  status?: "OPEN" | "IN_REVIEW" | "ANSWERED" | "CLOSED" | string;
  adminResponse?: string;
  createdAt?: string;
  updatedAt?: string;
};
